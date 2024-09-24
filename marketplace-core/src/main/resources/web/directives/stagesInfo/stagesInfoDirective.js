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

define( [ 'marketplaceApp', 'marketplace-lib/Logger' ],
    function ( app, logger ) {
      logger.log("Required stagesInfo/stagesInfoDirective.js");

      app.directive('stagesInfo',
          function() {
            return {
              restrict: 'A', // 'A' must be used for IE8 compatibility
              replace: true, //replaces the custom directive element with the corresponding expanded HTML, to be HTML-compliant.
              templateUrl: 'directives/stagesInfo/stagesInfoTemplate.html',
              controller: 'stagesInfoController',
              //isolate scope
              scope: {
                //'&' evaluates in the parent scope
                //'@' evaluates as a string
                //'=' evaluates in the isolate scope
              }
            };
          }
      );

    }
);
