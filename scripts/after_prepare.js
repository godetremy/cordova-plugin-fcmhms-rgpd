#!/usr/bin/env node

'use strict';

var fs = require('fs');
var path = require('path');

fs.ensureDirSync = function (dir) {
    if (!fs.existsSync(dir)) {
        dir.split(path.sep).reduce(function (currentPath, folder) {
            currentPath += folder + path.sep;
            if (!fs.existsSync(currentPath)) {
                fs.mkdirSync(currentPath);
            }
            return currentPath;
        }, '');
    }
};

var config = fs.readFileSync('config.xml').toString();
var name = getValue(config, 'name');

var IOS_DIR = 'platforms/ios';
var ANDROID_DIR = 'platforms/android';

var PLATFORM = {
    IOS: {
        dest: [
            IOS_DIR + '/' + name + '/Resources/GoogleService-Info.plist',
            IOS_DIR + '/' + name + '/Resources/Resources/GoogleService-Info.plist'
        ],
        src: [
            'GoogleService-Info.plist',
            IOS_DIR + '/www/GoogleService-Info.plist',
            'www/GoogleService-Info.plist'
        ]
    },
    GMS: {
        dest: [
            ANDROID_DIR + '/google-services.json',
            ANDROID_DIR + '/app/google-services.json'
        ],
        src: [
            'google-services.json',
            ANDROID_DIR + '/assets/www/google-services.json',
            'www/google-services.json'
        ],
        stringsXml: fileExists(ANDROID_DIR + '/app/src/main/res/values/strings.xml') ? ANDROID_DIR + '/app/src/main/res/values/strings.xml' : ANDROID_DIR + '/res/values/strings.xml'
    },
    HMS: {
        dest: [
            ANDROID_DIR + '/agconnect-services.json',
            ANDROID_DIR + '/app/agconnect-services.json'
        ],
        src: [
            'agconnect-services.json',
            ANDROID_DIR + '/assets/www/agconnect-services.json',
            'www/agconnect-services.json'
        ],
        stringsXml: fileExists(ANDROID_DIR + '/app/src/main/res/values/strings.xml') ? ANDROID_DIR + '/app/src/main/res/values/strings.xml' : ANDROID_DIR + '/res/values/strings.xml'
    }
};

function copyKey(platform, callback) {
    for (var i = 0; i < platform.src.length; i++) {
        var file = platform.src[i];
        if (fileExists(file)) {
            try {
                var contents = fs.readFileSync(file).toString();

                try {
                    platform.dest.forEach(function (destinationPath) {
                        var folder = destinationPath.substring(0, destinationPath.lastIndexOf('/'));
                        fs.ensureDirSync(folder);
                        fs.writeFileSync(destinationPath, contents);
                    });
                } catch (e) {
                    // skip
                }

                callback && callback(contents);
            } catch (err) {
                console.log(err);
            }

            break;
        }
    }
}

function getValue(config, name) {
    var value = config.match(new RegExp('<' + name + '>(.*?)</' + name + '>', 'i'));
    if (value && value[1]) {
        return value[1];
    } else {
        return null;
    }
}

function fileExists(path) {
    try {
        return fs.statSync(path).isFile();
    } catch (e) {
        return false;
    }
}

function directoryExists(path) {
    try {
        return fs.statSync(path).isDirectory();
    } catch (e) {
        return false;
    }
}

function stripDebugSymbols() {
    var podFilePath = 'platforms/ios/Podfile',
        podFile = fs.readFileSync(path.resolve(podFilePath)).toString();
    if (!podFile.match('DEBUG_INFORMATION_FORMAT')) {
        podFile += "\npost_install do |installer|\n" +
            "    installer.pods_project.targets.each do |target|\n" +
            "        target.build_configurations.each do |config|\n" +
            "            config.build_settings['DEBUG_INFORMATION_FORMAT'] = 'dwarf'\n" +
            "        end\n" +
            "    end\n" +
            "end";
        fs.writeFileSync(path.resolve(podFilePath), podFile);
        console.log('cordova-plugin-fcmhms-rgpd: Applied IOS_STRIP_DEBUG to Podfile');
    }
}

module.exports = function (context) {
    //get platform from the context supplied by cordova
    var platforms = context.opts.platforms;
    // Copy key files to their platform specific folders
    if (platforms.indexOf('ios') !== -1 && directoryExists(IOS_DIR)) {
        console.log('Preparing fcmhms on iOS');
        copyKey(PLATFORM.IOS);
        stripDebugSymbols();
    }
    if (platforms.indexOf('android') !== -1 && directoryExists(ANDROID_DIR)) {
        console.log('Preparing fcmhms on Android for GMS');
        copyKey(PLATFORM.GMS);
    }
    if (platforms.indexOf('android') !== -1 && directoryExists(ANDROID_DIR)) {
        console.log('Preparing fcmhms on Android for HMS');
        copyKey(PLATFORM.HMS);
    }
};
