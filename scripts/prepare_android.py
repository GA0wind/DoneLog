"""Prepare an isolated native project; downloads dependencies, never uploads source."""
import argparse
import json
import pathlib
import shutil
import urllib.request
import zipfile


def unpack(url, archive, destination, marker):
    if marker.exists():
        return
    if not archive.exists():
        print(f'Downloading {archive.name}', flush=True)
        temporary = archive.with_suffix('.download')
        urllib.request.urlretrieve(url, temporary)
        temporary.replace(archive)
    with zipfile.ZipFile(archive) as bundle:
        root = destination.resolve()
        for member in bundle.infolist():
            target = (root / member.filename).resolve()
            if not target.is_relative_to(root):
                raise ValueError('Archive entry is outside the dependency directory')
        bundle.extractall(destination)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--project', required=True, type=pathlib.Path)
    parser.add_argument('--android-sdk', required=True, type=pathlib.Path)
    args = parser.parse_args()
    project = args.project.resolve()
    cache = project / 'unpackage/local-build'
    cache.mkdir(parents=True, exist_ok=True)
    sdk = cache / 'sdk/Android-uni-app-x-SDK@15179-5.34'
    unpack('https://web-ext-storage.dcloud.net.cn/uni-app-x/sdk/Android/Android-uni-app-x-SDK%4015179-5.34.zip',
           cache / 'uni-app-x-sdk-5.34.zip', cache / 'sdk', sdk / 'SDK/libs/framework-release.aar')
    unpack('https://mirrors.cloud.tencent.com/gradle/gradle-8.14.3-bin.zip',
           cache / 'gradle-8.14.3-bin.zip', cache / 'tools', cache / 'tools/gradle-8.14.3/bin/gradle.bat')
    exported = project / 'unpackage/resources/app-android'
    manifest = json.loads((exported / '__UNI__7A2B275/www/manifest.json').read_text(encoding='utf-8-sig'))
    if manifest['uni-app-x']['compilerVersion'] != '5.34':
        raise RuntimeError('This build script requires HBuilderX / SDK 5.34. Export with the matching IDE.')
    native = cache / 'android'
    native.mkdir(exist_ok=True)
    shutil.copytree(project / 'scripts/android', native, dirs_exist_ok=True)
    (native / 'local.properties').write_text(f'sdk.dir={args.android_sdk.resolve().as_posix()}\n', encoding='utf-8')
    with (native / 'gradle.properties').open('a', encoding='utf-8') as handle:
        handle.write(f'\ndcloudSdk={sdk.as_posix()}\n')
        handle.write(f'donelogVersionName={manifest["version"]["name"]}\n')
        handle.write(f'donelogVersionCode={manifest["version"]["code"]}\n')

    def reset(relative):
        target = (native / relative).resolve()
        # Only regenerate explicitly scoped files inside this generated project.
        if not target.is_relative_to(native.resolve()) or target == native.resolve():
            raise ValueError(f'Unsafe generated directory: {target}')
        if target.exists():
            shutil.rmtree(target)
        target.mkdir(parents=True)
        return target

    shutil.copytree(exported / 'uniappx/app-android/src', reset('uniappx/src/main/java'), dirs_exist_ok=True)
    assets = reset('uniappx/src/main/assets/apps')
    shutil.copytree(exported / '__UNI__7A2B275', assets / '__UNI__7A2B275')
    widget = exported / 'uni_modules/donelogWidget/utssdk/app-android'
    if not (widget / 'src/index.kt').exists():
        raise RuntimeError('Widget was not exported as a UTS module. Import @/uni_modules/donelogWidget.')
    shutil.copytree(widget / 'src', reset('donelogWidget/src/main/java'), dirs_exist_ok=True)
    shutil.copytree(widget / 'res', reset('donelogWidget/src/main/res'), dirs_exist_ok=True)
    shutil.copy2(widget / 'AndroidManifest.xml', native / 'donelogWidget/src/main/AndroidManifest.xml')
    icon = native / 'app/src/main/res/drawable'
    icon.mkdir(parents=True, exist_ok=True)
    shutil.copy2(project / 'static/logo.png', icon / 'donelog_logo.png')
    print(f'Prepared local Android project: {native}', flush=True)


if __name__ == '__main__':
    main()
