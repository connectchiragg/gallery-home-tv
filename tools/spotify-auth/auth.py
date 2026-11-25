#!/usr/bin/env python3
"""One-time Spotify sign-in for the launcher's taste features.

Reads spotify_client_id / spotify_client_secret from ../../voice.json, opens Spotify's consent page,
catches the redirect on http://127.0.0.1:8888/callback (add that exact Redirect URI in your app's settings),
and writes spotify_refresh_token into voice.json. Scopes are read-only: top artists/tracks, recently played,
saved tracks, playlists. Re-run if the token is revoked or expires (Spotify: 180 days without use)."""
import base64, http.server, json, os, secrets, sys, urllib.parse, urllib.request, webbrowser

HERE = os.path.dirname(os.path.abspath(__file__))
CFG = os.path.join(HERE, '..', '..', 'voice.json')
REDIRECT = 'http://127.0.0.1:8888/callback'
SCOPES = 'user-top-read user-read-recently-played user-library-read playlist-read-private'

cfg = json.load(open(CFG))
cid, sec = cfg['spotify_client_id'], cfg['spotify_client_secret']
state = secrets.token_urlsafe(16)
url = 'https://accounts.spotify.com/authorize?' + urllib.parse.urlencode(
    {'client_id': cid, 'response_type': 'code', 'redirect_uri': REDIRECT, 'scope': SCOPES, 'state': state})
got = {}

class H(http.server.BaseHTTPRequestHandler):
    def log_message(self, *a): pass
    def do_GET(self):
        q = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
        ok = q.get('state', [''])[0] == state and 'code' in q
        got['code'] = q['code'][0] if ok else None
        got['error'] = q.get('error', [''])[0]
        self.send_response(200); self.send_header('Content-Type', 'text/html'); self.end_headers()
        self.wfile.write(b'<h2>Done. You can close this tab and go back to Claude.</h2>' if ok else b'<h2>Sign-in failed. Check the terminal.</h2>')

srv = http.server.HTTPServer(('127.0.0.1', 8888), H)
print('Open this link if the browser did not open:\n' + url, flush=True)
webbrowser.open(url)
while 'code' not in got:
    srv.handle_request()
if not got['code']:
    sys.exit('sign-in failed: ' + (got['error'] or 'state mismatch'))
req = urllib.request.Request('https://accounts.spotify.com/api/token', data=urllib.parse.urlencode(
    {'grant_type': 'authorization_code', 'code': got['code'], 'redirect_uri': REDIRECT}).encode(),
    headers={'Authorization': 'Basic ' + base64.b64encode(f'{cid}:{sec}'.encode()).decode()})
tok = json.load(urllib.request.urlopen(req))
cfg['spotify_refresh_token'] = tok['refresh_token']
json.dump(cfg, open(CFG, 'w'), indent=2)
me = json.load(urllib.request.urlopen(urllib.request.Request('https://api.spotify.com/v1/me/top/artists?limit=3',
    headers={'Authorization': 'Bearer ' + tok['access_token']})))
print('saved spotify_refresh_token; your top artists:', ', '.join(a['name'] for a in me.get('items', [])))
