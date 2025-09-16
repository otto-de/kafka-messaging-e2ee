#!/usr/bin/env bash

#set -vx

set -e
SCRIPT_DIR=$(dirname "$0")

USER_GRADLE_PROPERTIES=$HOME/.gradle/gradle.properties

function check_configured() {
    grep -q "$1" "${USER_GRADLE_PROPERTIES}" || echo "$1 is not configured in ${USER_GRADLE_PROPERTIES}. $2"
}

function check_configuration() {
  if [ ! -f "${USER_GRADLE_PROPERTIES}" ]; then
    echo "${USER_GRADLE_PROPERTIES} does not exist"
    exit 1
  fi

  check_configured "sonatypeUsername" "This is the username you use to authenticate with central.sonatype.com (e.g. otto-de)"
  check_configured "sonatypePassword" "This is the password you use to authenticate with central.sonatype.com (ask Guido or one of the developers)"
}

check_configuration

function createArtefacts() {
  echo "[INFO] create artefacts ..."
  if ! "${SCRIPT_DIR}"/gradlew clean check publish
  then
    echo "[ERROR] gradlew failed !"
    exit 1
  fi
  echo "[INFO] release artefacts were created at build/staging-deploy."
}

function signArtefacts() {
  echo "[INFO] sign artefacts ..."
  find "${SCRIPT_DIR}/build/staging-deploy" -type f -name '*.jar' -exec gpg --armor --detach-sign {} \;
  find "${SCRIPT_DIR}/build/staging-deploy" -type f -name '*.module' -exec gpg --armor --detach-sign {} \;
  find "${SCRIPT_DIR}/build/staging-deploy" -type f -name '*.pom' -exec gpg --armor --detach-sign {} \;
}

function createDeploymentBundle() {
  echo "[INFO] create bundle file ..."
  if ! "${SCRIPT_DIR}"/gradlew zipForUpload
  then
    echo "[ERROR] gradlew failed !"
  fi
  echo "[INFO] file build/staging-deploy/central-bundle.zip created."
}

function cleanupCurlOut() {
  rm -f curlout.txt
#  echo "rm curlout.txt"
}
trap cleanupCurlOut EXIT

function currentDeploymentState() {
  apiToken="$1"
  deploymentId="$2"

  curl -o curlout.txt --silent --request POST --header "Authorization: Bearer ${apiToken}" \
      "https://central.sonatype.com/api/v1/publisher/status?id=${deploymentId}"
  jq -r '.deploymentState' < curlout.txt
}

function uploadDeployment() {
  uploadType="$1"

  echo "[INFO] upload bundle file ..."
  sonatypeUsername=$(grep 'sonatypeUsername=' "${USER_GRADLE_PROPERTIES}" | sed 's/sonatypeUsername=//')
  sonatypePassword=$(grep 'sonatypePassword=' "${USER_GRADLE_PROPERTIES}" | sed 's/sonatypePassword=//')
  apiToken=$(echo -n "${sonatypeUsername}:${sonatypePassword}" | base64)

  # see: https://central.sonatype.org/publish/publish-portal-api
  curl -o curlout.txt --silent --request POST --header "Authorization: Bearer ${apiToken}" \
      --form bundle=@build/staging-deploy/central-bundle.zip \
      "https://central.sonatype.com/api/v1/publisher/upload?publishingType=${uploadType}"
  deploymentId=$(cat curlout.txt)
  if [ "${deploymentId}" == "" ]
  then
    echo "[ERROR] deployment was not started"
    cat curlout.txt
    echo ""
    exit 1
  fi
  echo "[INFO] deployment ${deploymentId} started."

  ## wait for deployment to finish
  sleep 15
  curl -o curlout.txt --silent --request POST --header "Authorization: Bearer ${apiToken}" \
      "https://central.sonatype.com/api/v1/publisher/status?id=${deploymentId}"

  MAX_WAIT_TIME=900
  CURRENT_WAIT_TIME=0
  CURRENT_STATE=$(currentDeploymentState "${apiToken}" "${deploymentId}" )
  while [ "${CURRENT_STATE}" != "FAILED" ] && [ "${CURRENT_STATE}" != "VALIDATED" ] && [ "${CURRENT_STATE}" != "PUBLISHED" ]
  do
    if [ $CURRENT_WAIT_TIME -ge $MAX_WAIT_TIME ]
    then
      echo "Maximum wait time exceeded. Aborting."
      exit 1
    fi
    echo "✘ Expected state not yet available (currently ${CURRENT_STATE}) ..."
    sleep 15
    (( CURRENT_WAIT_TIME+=15 ))
    CURRENT_STATE=$(currentDeploymentState "${apiToken}" "${deploymentId}" )
  done

  if [ "${CURRENT_STATE}" != "PUBLISHED" ]
  then
    echo "✘ deployment failed. Error response:"
    cat curlout.txt
    echo ""
  fi

  if [ "${CURRENT_STATE}" == "FAILED" ] || [ "${CURRENT_STATE}" == "VALIDATED" ]
  then
    echo "delete deployment ${deploymentId} from central.sonatype.com ..."
    curl -o curlout.txt --silent --request DELETE --header "Authorization: Bearer ${apiToken}" \
        "https://central.sonatype.com/api/v1/publisher/deployment/${deploymentId}"
  fi

  if [ "${CURRENT_STATE}" != "PUBLISHED" ]
  then
    exit 1
  else
    echo "✔︎ Deployment finished"
  fi
}

set +e
grep 'version = .*-SNAPSHOT' "$SCRIPT_DIR/gradle.properties"
SNAPSHOT=$?
set -e

createArtefacts
signArtefacts
createDeploymentBundle

if [[ $SNAPSHOT == 1 ]]; then
  echo "upload an release none snapshot version to central.sonatype.com via publish portal API ..."
  uploadDeployment "AUTOMATIC"
  #uploadDeployment "USER_MANAGED"
else
  echo "upload snapshot version to central.sonatype.com via publish portal API ..."
  uploadDeployment "USER_MANAGED"
fi

