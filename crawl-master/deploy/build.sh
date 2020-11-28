#!/bin/bash

set -e

owner=supernovaunwired

ebt "universal:packageBin"
version=$(sed  -E 's/version\ ?:=\ ?\"([0-9]\.[0-9])\"/\1/' <../version.sbt)
zipfile="../target/universal/crawl-master-$version.zip"

unzip "$zipfile"
mv "crawl-master-$version" target

tag="$owner/crawl-master:$version"

if command -v podman >/dev/null; then
	podman build -t "$tag" .
else
	docker build -t "$tag" .
fi

if [ ! -f build.sbt ]; then
	echo "Removing target"
	rm -rf target
fi
