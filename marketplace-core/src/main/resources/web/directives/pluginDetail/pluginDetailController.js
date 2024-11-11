/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


'use strict';


define( [
      'marketplaceApp',
      'underscore',
      'marketplace-lib/Logger'
    ],
    function ( app, _, logger ) {

      logger.log("Required pluginDetail/pluginDetailController.js");

      app.controller( 'pluginDetailController',
          ['$scope', 'installFlowService', '$translate',
            function ( $scope, installService, $translate ) {
              // Add trim to String if it is not defined
              if (!('trim' in String.prototype)) {
                String.prototype.trim= function() {
                  return this.replace(/^\s+/, '').replace(/\s+$/, '');
                };
              }

              function isEmptyString( string ) {
                return string == undefined ||
                    string == null ||
                    string.trim() == '';
              }

              function Info (label, description, url) {
                this.label = label;
                this.description = description;
                this.url = url;
              }

              Info.prototype.hasDescription = function () {
                return !isEmptyString( this.description );
              };

              Info.prototype.hasUrl = function () {
                return !isEmptyString( this.url );
              };

              function CreateInfo ( label, description, url ) {
                return new Info( label,description, url );
              }


              function getPluginInformation( plugin ) {
                var selectedVersion = $scope.selectedPluginVersion;

                var versionInfo = [];
                if ( selectedVersion ) {
                  versionInfo = [
                    CreateInfo( $translate.instant('marketplace.pluginDetail.section.information.branch'),
                        selectedVersion.branch),
                    CreateInfo( $translate.instant('marketplace.pluginDetail.section.information.version'),
                        selectedVersion.version),
                    CreateInfo( $translate.instant('marketplace.pluginDetail.section.information.build'),
                        selectedVersion.build),
                    CreateInfo( $translate.instant('marketplace.pluginDetail.section.information.releaseDate'),
                        selectedVersion.releaseDate ),
                    // TODO: These infos aren't in the mock
                    // newInfo( "Name", selectedVersion.name ),
                    // newInfo( "Samples", "samples", selectedVersion.samplesDownloadUrl ),
                    // newInfo( "Version Description",  selectedVersion.description ),
                    // newInfo( "ChangeLog", version.changeLog ),
                  ];
                }

                var infos = [
                    CreateInfo( $translate.instant('marketplace.pluginDetail.section.information.developer'),
                        plugin.author.name, plugin.author.siteUrl ),
                    CreateInfo( $translate.instant('marketplace.pluginDetail.section.information.license'),
                        plugin.license.name ),
                    CreateInfo( $translate.instant('marketplace.pluginDetail.section.information.dependencies'),
                        plugin.dependencies)
                ];

                // remove information with empty description as they should not be in the view
                return _.filter( versionInfo.concat(infos), function( info ) {  return info.hasDescription(); } );

              }

              function getDisagreeMailTranslationObject() {
                var version = $scope.selectedPluginVersion;
                return {
                  stageLane:  version.devStage.lane.name,
                  stagePhase: version.devStage.phase,
                  versionBranch: version.branch,
                  versionVersion: version.version,
                  versionBuild: version.buildId,
                  pluginName: $scope.plugin.name
                }
              }

              function selectVersion( version ) {
                $scope.selectedPluginVersion = version;
              }

              $scope.selectVersion = selectVersion;

              $scope.uninstallPlugin = installService.uninstallPlugin;
              $scope.pluginInformation = getPluginInformation( $scope.plugin );

              $scope.selectedPluginVersion = $scope.plugin.versions[0];

              // update version info when a new version is selected
              $scope.$watch( 'selectedPluginVersion', function () {
                    $scope.pluginInformation = getPluginInformation( $scope.plugin );
                    $scope.disagreeMailTranslationVars = getDisagreeMailTranslationObject();
                  }
              );

            }
          ]);
    }
);
