import dropbox
from dropbox import DropboxOAuth2FlowNoRedirect

APP_KEY    = "92n3j2k1ogmylwx"       # from Dropbox App Settings tab
APP_SECRET = "945muz7rpc5vhlf"    # from Dropbox App Settings tab

auth_flow = DropboxOAuth2FlowNoRedirect(
    APP_KEY, APP_SECRET,
    token_access_type="offline"   # ← this gets you a permanent refresh_token
)

authorize_url = auth_flow.start()
print("1. Go to:", authorize_url)
print("2. Click Allow, then copy the authorization code")
auth_code = input("3. Paste the code here: ").strip()

oauth_result = auth_flow.finish(auth_code)
print("\n✅ Save these in your secrets.toml:")
print(f'   app_key     = "{APP_KEY}"')
print(f'   app_secret  = "{APP_SECRET}"')
print(f'   refresh_token = "{oauth_result.refresh_token}"')