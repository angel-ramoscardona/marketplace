/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/



'use strict';

define(
    [
      'marketplaceApp',
      'underscore',
      'marketplace-lib/Logger'
    ],
    function ( app, _, logger ) {
      logger.log("Required services/developmentStageService.js");

      var service = app.factory( 'developmentStageService',
          [ '$translate',
            function ( $translate ) {

              var customerLaneId = "Customer";
              var communityLaneId = "Community";

              var lanes = {};

              function Lane ( id, name ) {
                this.id = id;
                this.name = name;

                // associative map [phaseId: stage]
                this.stages = {};
              }

              function createLane( laneId, laneNameId ) {
                var lane = new Lane( laneId );

                //lane.name = $translate.instant( laneNameId );

                $translate( laneNameId )
                    .then( function ( name ) {
                      lane.name = name;
                    });

                // TODO: check if add here or out of function
                lanes[laneId] = lane;

                return lane;
              }

              function initializeLanes() {
                createLane( customerLaneId, 'marketplace.devStage.lanes.customer.name' );
                createLane( communityLaneId, 'marketplace.devStage.lanes.community.name' );
              }

              function getLane( laneId ) {
                return lanes[laneId];
              }

              /**
               *
               * @param lane
               * @param phaseId
               * @param name
               * @param description
               * @constructor
               */
              function DevelopmentStage ( lane, phaseId, name, description ) {
                this.lane = lane;
                this.phase = phaseId;

                this.name = name;
                this.description = description;
              }

              DevelopmentStage.stagesRank = {
                Community: { 1: 1, 2: 2, 3: 3, 4: 4 },
                Customer: { 1: 5, 2: 6, 3: 7, 4: 8 }
              };

              DevelopmentStage.prototype.getRank = function() {
                return DevelopmentStage.stagesRank[this.lane.id][this.phase];
              }

              function createDevelopmentStage( laneId, phaseId, nameTranslationId, descriptionTrasnlationId ) {
                var lane = getLane( laneId );
                if( lane === undefined ) {
                  throw "createDevelopmentStage: unknown laneId " + laneId;
                }

                var stage = new DevelopmentStage( lane, phaseId );

//                stage.name =  $translate.instant( nameTranslationId );
//                stage.description = $translate.instant( descriptionTrasnlationId );

                $translate( nameTranslationId )
                    .then(function (name) {
                      stage.name = name;
                    });
                $translate( descriptionTrasnlationId )
                    .then(function (description) {
                      stage.description = description;
                    });


                // TODO: check if add here or out of function
                lane.stages[phaseId] = stage;

                return stage;
              }

              // TODO: check if these should be obtained from metadata
              function intializeDevelopmentStages() {

                function createStages ( numberOfStages, laneId, translationIdPrefix ) {
                  for (var phaseId = 1; phaseId <= numberOfStages; phaseId++) {
                    createDevelopmentStage( laneId, phaseId,
                            translationIdPrefix + phaseId + ".name",
                            translationIdPrefix + phaseId + ".description")
                  }
                }

                initializeLanes();

                var translationKeyPrefix = "marketplace.devStage.stages";
                var customerLanePrefix = translationKeyPrefix + ".customer.phase";
                var communityLanePrefix = translationKeyPrefix + ".community.phase";

                createStages( 4, customerLaneId, customerLanePrefix );
                createStages( 4, communityLaneId, communityLanePrefix );
              }

              /**
               * Gets the stage for the corresponding lane and phase
               * @param laneId
               * @param phaseId
               * @returns {*} Returns undefined if no Stage was found for the specified lane and phase
               */
              function getStage( laneId, phaseId ) {
                if ( !laneId || !phaseId ) {
                  return undefined;
                }

                var lane = getLane( laneId );
                if ( lane === undefined ) {
                  return undefined;
                }

                return lane.stages[phaseId];
              }

              function getStages () {
                return _.chain( lanes )
                    .map( function( lane ) { return lane.stages; } )
                    .map( function (stages) { return _.toArray( stages ); })
                    .flatten()
                    // IE8 requires that undefined and null values are explicitly removed
                    .filter( function ( stage ) { return !(stage === undefined || stage === null); } )
                    .value();
              }

              function getLanes () {
                return _.map( lanes , function ( lane ) { return lane; } );
              }

              intializeDevelopmentStages();
              return {
                getStage: getStage,

                getStages: getStages,

                getLanes: getLanes

              }
            }
          ]);

      return service;
    }
);

