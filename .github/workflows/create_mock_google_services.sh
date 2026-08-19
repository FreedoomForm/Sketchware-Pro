#!/usr/bin/env bash
# Creates a mock google-services.json file if the real one is not provided
# via the GOOGLE_SERVICES_JSON secret. Used by GitHub Actions workflows
# so the build can run without Firebase configured.
set -e

if [ -n "$GOOGLE_SERVICES_JSON" ]; then
    printf 'GOOGLE_SERVICES_JSON<<EOF\n%s\nEOF\n' "$GOOGLE_SERVICES_JSON" >> $GITHUB_ENV
    exit 0
fi

mkdir -p app
cat > app/google-services.json << 'JSON'
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "mock",
    "storage_bucket": "mock.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000000",
        "android_client_info": {
          "package_name": "pro.sketchware"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "mock_AIzaSyBQJCUXVKUjD38-u5pWkqMtesIfFvxAcvs"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
JSON
