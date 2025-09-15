#!/usr/bin/env bash

# set -vx

if [[ $# -ne 1 ]]; then
  echo "Usage: ${0} NEW_VERSION"
  echo "  e.g. ${0} 1.5.2"
  exit 1
fi

nextReleaseVersion="$1"

sed --debug -i "s/^version.*/version = ${nextReleaseVersion}/g" gradle.properties
sed --debug -i -r "s/^(.*)de.otto:kafka-messaging-e2ee:[0-9.]+(.*)/\1de.otto:kafka-messaging-e2ee:${nextReleaseVersion}\2/g" docs/USAGE.md
