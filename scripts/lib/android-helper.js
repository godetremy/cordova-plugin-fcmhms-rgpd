var fs = require("fs");
var path = require("path");
var utilities = require("./utilities");

module.exports = {

  addFabricBuildToolsGradle: function () {

    var buildGradle = utilities.readBuildGradle();

    var addToBuildGradle = [
      "",
      "// Fabric Cordova Plugin - Start Fabric Build Tools ",
      "buildscript {",
      "  repositories {",
      "      google()",
      "      // huawei maven",
      "      maven {",
      "        url 'https://developer.huawei.com/repo/'",
      "      }",
      "  }",
      "  dependencies {",
      "    classpath 'com.android.tools.build:gradle:3.6.0'",
      "    classpath 'com.google.gms:google-services:+'  // Google Services plugin",
      "    classpath 'com.huawei.agconnect:agcp:+'  // HUAWEI agcp plugin",
      "  }",
      "}",
      "",
      "// Fabric Cordova Plugin - End Fabric Build Tools"
    ].join("\n");

    buildGradle = buildGradle.replace(/(\/\/ PLUGIN GRADLE EXTENSIONS START)/, addToBuildGradle + '\n\n$1');

    utilities.writeBuildGradle(buildGradle);
  },

  removeFabricBuildToolsFromGradle: function () {

    var buildGradle = utilities.readBuildGradle();

    buildGradle = buildGradle.replace(/\n\/\/ Fabric Cordova Plugin - Start Fabric Build Tools[\s\S]*\/\/ Fabric Cordova Plugin - End Fabric Build Tools/, "");

    utilities.writeBuildGradle(buildGradle);
  }
};