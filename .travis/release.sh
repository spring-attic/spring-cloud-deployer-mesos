#! /bin/bash
set -e

# Install github_changelog_generator
gem install github_changelog_generator

# Generate CHANGELOG.md
echo -e "\033[0;32mGenerating CHANGELOG...\033[0m"
github_changelog_generator -t ${GITHUB_ACCESS_TOKEN}

# Commit and push the CHANGELOG
git config --global user.email "development@trustedchoice.com"
git config --global user.name "TrustedChoice.com Buildmaster"
git remote rm origin
git remote add origin https://${GITHUB_USERNAME}:${GITHUB_ACCESS_TOKEN}@github.com/${TRAVIS_REPO_SLUG}.git
git add CHANGELOG.md
git commit -m "Add CHANGELOG [ci skip]" && git push origin HEAD:master

# Get latest (soon to be previous) release
previous_release_tag=$(curl -s \
    -u ${GITHUB_USERNAME}:${GITHUB_ACCESS_TOKEN} \
    https://api.github.com/repos/${TRAVIS_REPO_SLUG}/releases/latest | \
        jq -r .tag_name)

# Create GitHub release
echo -e "\033[0;32mCreating GitHub release...\033[0m"
release_name="${TRAVIS_TAG:1}"
github_changelog_generator -t ${GITHUB_ACCESS_TOKEN} -o target/CHANGELOG.md --since-tag ${previous_release_tag}
cat <(sed -e '$ d' target/CHANGELOG.md) <(echo "Bintray artifacts: https://bintray.com/trustedchoice/spring-cloud-dataflow-mesos/spring-cloud-deployer-mesos/${release_name}") > target/CHANGELOG.md.release
release_changelog=$(< target/CHANGELOG.md.release)
release_id=$(jq -n \
    --arg tag_name "$TRAVIS_TAG" \
    --arg release_name "$release_name" \
    --arg release_changelog "$release_changelog" \
    '{
        "tag_name": $tag_name,
        "name": $release_name,
        "body": $release_changelog,
        "draft": false,
        "prerelease": false
    }' |
    curl -s \
        -u ${GITHUB_USERNAME}:${GITHUB_ACCESS_TOKEN} \
        -d@- \
        https://api.github.com/repos/${TRAVIS_REPO_SLUG}/releases | jq -r .id)

# mvn deploy to Bintray
echo -e "\033[0;32mDeploying to Bintray...\033[0m"
./mvnw \
    org.codehaus.mojo:build-helper-maven-plugin:3.0.0:parse-version \
    versions:set -DnewVersion="${release_name}" \
    versions:commit
./mvnw --settings .settings.xml -DskipTests=true deploy

# Increment, commit and push the next development version
echo -e "\033[0;32mSetting next development version...\033[0m"
./mvnw \
    org.codehaus.mojo:build-helper-maven-plugin:3.0.0:parse-version \
    versions:set -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}-SNAPSHOT \
    versions:commit
git add pom.xml
git commit -m "Setting next development version [ci skip]" && git push origin HEAD:master