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


package org.pentaho.marketplace.endpoints;

import org.pentaho.marketplace.domain.model.entities.interfaces.IDomainStatusMessage;
import org.pentaho.marketplace.domain.model.entities.interfaces.IPlugin;
import org.pentaho.marketplace.domain.services.interfaces.IRDO;
import org.pentaho.marketplace.endpoints.dtos.mappers.interfaces.IPluginDTOMapper;
import org.pentaho.marketplace.endpoints.dtos.mappers.interfaces.IStatusMessageDTOMapper;
import org.pentaho.marketplace.endpoints.dtos.responses.IterablePluginOperationResultDTO;
import org.pentaho.marketplace.endpoints.dtos.responses.StringOperationResultDTO;
import org.pentaho.marketplace.endpoints.dtos.responses.base.OperationResultDTO;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Path( "services" )
public class MarketplaceService {

  private IRDO RDO;
  private IPluginDTOMapper pluginDTOMapper;
  private IStatusMessageDTOMapper statusMessageDTOMapper;

  public MarketplaceService( IRDO rdo,
                             IPluginDTOMapper pluginDTOMapper,
                             IStatusMessageDTOMapper statusMessageDTOMapper ) {

    //dependency obtained via constructor dependency injection from spring framework
    this.RDO = rdo;
    this.pluginDTOMapper = pluginDTOMapper;
    this.statusMessageDTOMapper = statusMessageDTOMapper;
  }

  @GET
  @Path( "/hello" )
  @Produces( MediaType.APPLICATION_JSON )
  public StringOperationResultDTO hello() {

    //create response object
    StringOperationResultDTO result = new StringOperationResultDTO();
    result.string = "Hello World from Marketplace!";

    //status message
    result.statusMessage.code = "OK_CODE";
    result.statusMessage.message = "OK_MESSAGE";

    //return string
    return result;
  }

  @GET
  @Path( "/plugins" )
  @Produces( MediaType.APPLICATION_JSON )
  public IterablePluginOperationResultDTO getPlugins( @Context HttpServletResponse response ) {

    //get plugins from the domain model
    List<IPlugin> plugins = new ArrayList<>( this.RDO.getPluginService().getPlugins().values() );

    Collections.sort( plugins, new Comparator<IPlugin>() {
        @Override public int compare( IPlugin plugin1, IPlugin plugin2 ) {
          return plugin1.getRank() - plugin2.getRank();
        }
      }
    );

    //transform plugins to DTOs for serialization
    IterablePluginOperationResultDTO result = new IterablePluginOperationResultDTO();
    result.plugins = this.pluginDTOMapper.toDTOs( plugins );

    //status message
    result.statusMessage.code = "OK_CODE";
    result.statusMessage.message = "OK_MESSAGE";

    response.addHeader( "Cache-Control", "no-cache, no-store" );

    return result;
  }

  @POST
  @Path( "/plugin/{pluginId}/{versionBranch}" )
  @Produces( MediaType.APPLICATION_JSON )
  public OperationResultDTO installPlugin( @PathParam( "pluginId" ) String pluginId,
                                           @PathParam( "versionBranch" ) String versionBranch ) {
    OperationResultDTO result = new OperationResultDTO();
    //install plugin
    IDomainStatusMessage statusMessage = this.RDO.getPluginService().installPlugin( pluginId, versionBranch );

    //send installation string
    result.statusMessage = this.statusMessageDTOMapper.toDTO( statusMessage );
    return result;
  }

  @DELETE
  @Path( "/plugin/{pluginId}" )
  @Produces( MediaType.APPLICATION_JSON )
  public OperationResultDTO uninstallPlugin( @PathParam( "pluginId" ) String pluginId ) {

    OperationResultDTO result = new OperationResultDTO();
    //uninstall plugin
    IDomainStatusMessage statusMessage = this.RDO.getPluginService().uninstallPlugin( pluginId );

    //send installation string
    result.statusMessage = this.statusMessageDTOMapper.toDTO( statusMessage );
    return result;
  }
}
