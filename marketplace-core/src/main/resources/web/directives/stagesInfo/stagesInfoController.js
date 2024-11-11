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

define( [ 'marketplaceApp',
      'underscore',
      'marketplace-lib/Logger'
    ],
    function ( app, _, logger ) {
      logger.log("Required stagesInfo/stagesInfoController.js");

      app.controller('stagesInfoController',
          ['$scope', 'developmentStageService',
            function ( $scope, devStageService ) {
              $scope.lanes = devStageService.getLanes();
            }
          ]);
    }
);