#!/usr/bin/env python3
"""Run after `npx cap add android`: adds the music-library plugin, background-play service, permissions and icons."""
import os, re, shutil, sys
A = 'android'
main = f'{A}/app/src/main'
pkg = f'{main}/java/app/emitunes'
os.makedirs(pkg, exist_ok=True)
for f in os.listdir('native/java'):
    shutil.copy(f'native/java/{f}', f'{pkg}/{f}')

# manifest
mp = f'{main}/AndroidManifest.xml'
m = open(mp, encoding='utf-8').read()
if 'PlaybackService' not in m:
    perms = '''
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />'''
    m = m.replace('<uses-permission android:name="android.permission.INTERNET" />', '<uses-permission android:name="android.permission.INTERNET" />' + perms, 1)
    m = m.replace('</application>', '''    <service android:name=".PlaybackService" android:exported="false" android:foregroundServiceType="mediaPlayback" />
    </application>''', 1)
    assert 'PlaybackService' in m and 'READ_MEDIA_AUDIO' in m
    open(mp, 'w', encoding='utf-8').write(m)

# minSdk 26 (notification channels + media style without extra libraries)
vp = f'{A}/variables.gradle'
v = open(vp).read()
v = re.sub(r'minSdkVersion = \d+', 'minSdkVersion = 26', v)
open(vp, 'w').write(v)

# launcher icons
res = f'{main}/res'
shutil.rmtree(f'{res}/mipmap-anydpi-v26', ignore_errors=True)
for d in os.listdir(res):
    if d.startswith('mipmap-') and os.path.isdir(f'{res}/{d}') and os.path.isdir('native/icons'):
        src = f'native/icons/{d}'
        if os.path.isdir(src):
            for f in os.listdir(src):
                shutil.copy(f'{src}/{f}', f'{res}/{d}/{f}')
print('native files applied')
