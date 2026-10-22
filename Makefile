# Release helpers (see "Releasing" in README.md).
#
#   make test      run the tests
#   make build     build build/distributions/idea-cddl-<version>.zip
#   make sign      build, sign and verify build/distributions/idea-cddl-<version>-signed.zip
#   make publish   run the tests, then sign and upload a new version to JetBrains Marketplace
#
# sign and publish need SIGNING_DIR, the directory holding chain.crt, private_encrypted.pem and, optionally,
# publish-token. Set it on the command line or in local.mk (ignored by git):
#
#   SIGNING_DIR = /Volumes/<encrypted volume>/jetbrains-signing

-include local.mk
export SIGNING_DIR

.PHONY: help test build sign publish

help:
	@sed -n 's/^#   make /make /p' $(firstword $(MAKEFILE_LIST))

test:
	./gradlew check

build:
	./gradlew buildPlugin

sign:
	scripts/gradle-with-secrets.sh signPlugin verifyPluginSignature

publish: test
	scripts/gradle-with-secrets.sh --publish publishPlugin
