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


package org.pentaho.marketplace.domain.services;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.kar.KarService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationAdmin;
import org.pentaho.marketplace.domain.model.entities.MarketEntryType;
import org.pentaho.marketplace.domain.model.entities.interfaces.IDomainStatusMessage;
import org.pentaho.marketplace.domain.model.entities.interfaces.IPlugin;
import org.pentaho.marketplace.domain.model.entities.interfaces.IPluginVersion;
import org.pentaho.marketplace.domain.model.entities.serialization.MarketplaceXmlSerializer;
import org.pentaho.marketplace.domain.model.factories.DomainStatusMessageFactory;
import org.pentaho.marketplace.domain.model.factories.PluginFactory;
import org.pentaho.marketplace.domain.model.factories.PluginVersionFactory;
import org.pentaho.marketplace.domain.model.factories.VersionDataFactory;
import org.pentaho.marketplace.domain.model.factories.interfaces.IDomainStatusMessageFactory;
import org.pentaho.marketplace.domain.model.factories.interfaces.IPluginFactory;
import org.pentaho.marketplace.domain.model.factories.interfaces.IPluginVersionFactory;
import org.pentaho.marketplace.domain.model.factories.interfaces.IVersionDataFactory;
import org.pentaho.marketplace.domain.services.interfaces.IPluginProvider;
import org.pentaho.marketplace.domain.services.interfaces.IRemotePluginProvider;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISecurityHelper;
import org.pentaho.telemetry.ITelemetryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaPluginServiceTest {


  private String getSolutionPath() {
    return System.getProperty( "user.dir" ) + "/target/test-classes/pentaho-solutions/";
  }

  IDomainStatusMessageFactory domainStatusMessageFactory;
  IVersionDataFactory versionDataFactory;

  IPluginFactory pluginFactory = new PluginFactory();
  IPluginVersionFactory pluginVersionFactory = new PluginVersionFactory();

  //region auxiliary methods

  /**
   * Creates a new plugin service.
   * Solution folder is set to "test-res/pentaho-solutions/"
   * @return a new test plugin service
   */
  private BaPluginService createPluginService() {
    IDomainStatusMessageFactory domainStatusMessageFactory = this.domainStatusMessageFactory;
    IVersionDataFactory versionDataFactory = this.versionDataFactory;
    IPluginVersionFactory pluginVersionFactory = this.pluginVersionFactory;

    IRemotePluginProvider pluginProvider = mock( IRemotePluginProvider.class );
    MarketplaceXmlSerializer serializer = mock( MarketplaceXmlSerializer.class );
    ISecurityHelper securityHelper = mock( ISecurityHelper.class );
    ITelemetryService telemetryService = mock( ITelemetryService.class );
    KarService karService = mock( KarService.class );
    FeaturesService featuresService = mock( FeaturesService.class );
    Feature[] emtptyFeatures = {};
    try {
      when( featuresService.listInstalledFeatures() ).thenReturn( emtptyFeatures );
      when( featuresService.listFeatures() ).thenReturn( emtptyFeatures );
      when( featuresService.listRepositories() ).thenReturn( new Repository[0] );
    } catch ( Exception e ) {}
    Bundle bundle = mock( Bundle.class );
    ConfigurationAdmin configurationAdmin = mock( ConfigurationAdmin.class );

    BaPluginService service = new BaPluginService( pluginProvider, versionDataFactory, pluginVersionFactory,
      karService, featuresService, configurationAdmin, telemetryService, domainStatusMessageFactory, serializer, securityHelper, bundle );

    IApplicationContext applicationContext = mock( IApplicationContext.class );
    final String solutionPath = this.getSolutionPath();
    when( applicationContext.getSolutionPath( anyString() ) ).thenAnswer( new Answer<String>() {
      @Override public String answer( InvocationOnMock invocation ) throws Throwable {
        String path = (String) invocation.getArguments()[ 0 ];
        return solutionPath + path;
      }
    } );
    service.setApplicationContext( applicationContext );

    return service;
  }

  private Authentication createMockUserAuthentication( String role, String userName ) {
    GrantedAuthority userAuthority = mock( GrantedAuthority.class );
    when( userAuthority.getAuthority() ).thenReturn( role );
    Authentication userAuthentication = mock( Authentication.class );
    List<GrantedAuthority> authList = new ArrayList<GrantedAuthority>();
    authList.add( userAuthority );
    doReturn( authList ).when( userAuthentication ).getAuthorities();
    when( userAuthentication.getName() ).thenReturn( userName );

    return  userAuthentication;
  }

  // endregion

  @Before
  public void setup() {
    this.domainStatusMessageFactory = new DomainStatusMessageFactory();
    this.versionDataFactory = new VersionDataFactory();
  }

  // region Tests

  /**
   * Tests that when no allowed roles are defined and the user trying to install is not an administrator
   * then the installation is denied.
   */
  @Test
  public void testInstallDeniedNoRolesAndNonAdminUser( ) {
    // arrange
    BaPluginService service = this.createPluginService();

    // setup no roles
    service.setAuthorizedRoles( Collections.<String>emptyList() );

    // setup security helper for non admin user
    ISecurityHelper securityHelper = service.getSecurityHelper();
    when( securityHelper.isPentahoAdministrator( Mockito.any( IPentahoSession.class ) ) ).thenReturn( false );

    // act
    IDomainStatusMessage result = service.installPlugin( "doesNotMatter", "doesNotMatter" );

    // assert
    assertThat( result.getCode(), is( equalTo( BasePluginService.UNAUTHORIZED_ACCESS_ERROR_CODE ) ) );
  }

  /**
   * Tests that when no allowed roles are defined and the user trying to uninstall is not an administrator
   * then the uninstall is denied.
   */
  @Test
  public void testUninstallDeniedNoRolesAndNonAdminUser( ) {
    // arrange
    BaPluginService service = this.createPluginService();

    // setup no roles
    service.setAuthorizedRoles( Collections.<String>emptyList() );

    // setup security helper for non admin user
    ISecurityHelper securityHelper = service.getSecurityHelper();
    when( securityHelper.isPentahoAdministrator( Mockito.any( IPentahoSession.class ) ) ).thenReturn( false );

    // act
    IDomainStatusMessage result = service.uninstallPlugin( "doesNotMatter" );

    // assert
    assertThat( result.getCode(), is( equalTo( BasePluginService.UNAUTHORIZED_ACCESS_ERROR_CODE ) ) );
  }


  /**
   * Tests that when allowed roles are defined and the user trying to install does not have one of them
   * then the installation is denied.
   */
  @Test
  public void testInstallDeniedUserNotInRoles( ) {
    // region arrange
    BaPluginService service = this.createPluginService();

    // this is the role of the user which will be in the authorized roles
    String userRole = "peasant";
    Collection<String> authorizedRoles = Arrays.asList( "manager", "lackey" );

    // setup roles
    service.setAuthorizedRoles( authorizedRoles );

    Authentication userAuthentication = this.createMockUserAuthentication( userRole, null );
    // setup security helper
    ISecurityHelper securityHelper = service.getSecurityHelper();
    when( securityHelper.getAuthentication( Mockito.any( IPentahoSession.class ), eq( true ) ) ).thenReturn( userAuthentication );

    // endregion

    // act
    IDomainStatusMessage result = service.installPlugin( "doesNotMatter", "doesNotMatter" );

    // assert
    assertThat( result.getCode(), is( equalTo( BasePluginService.UNAUTHORIZED_ACCESS_ERROR_CODE ) ) );
  }

  /**
   * Tests that when allowed roles are defined and the user trying to uninstall does not have one of them
   * then the uninstall is denied.
   */
  @Test
  public void testUninstallDeniedUserNotInRoles( ) {
    // region arrange
    BaPluginService service = this.createPluginService();

    // this is the role of the user which will be in the authorized roles
    String userRole = "peasant";
    Collection<String> authorizedRoles = Arrays.asList( "manager", "lackey" );

    // setup roles
    service.setAuthorizedRoles( authorizedRoles );

    Authentication userAuthentication = this.createMockUserAuthentication( userRole, null );
    // setup security helper
    ISecurityHelper securityHelper = service.getSecurityHelper();
    when( securityHelper.getAuthentication( Mockito.any( IPentahoSession.class ), eq( true ) ) ).thenReturn( userAuthentication );

    // endregion

    // act
    IDomainStatusMessage result = service.uninstallPlugin( "doesNotMatter" );

    // assert
    assertThat( result.getCode(), is( equalTo( BasePluginService.UNAUTHORIZED_ACCESS_ERROR_CODE ) ) );
  }


  /**
   * Tests that when allowed users are defined and the user trying to install is not one of them
   * then the installation is denied.
   */
  @Test
  public void testInstallDeniedUserNotInUsers( ) {
    // arrange
    String userName = "Dennis";
    Collection<String> authorizedUsers = Arrays.asList( "Joseph", "David" );
    Collection<String> authorizedRoles = Collections.emptyList();
    BaPluginService service = this.createPluginService();

    service.setAuthorizedUsernames( authorizedUsers );
    service.setAuthorizedRoles( authorizedRoles );

    Authentication userAuthentication = this.createMockUserAuthentication( null, userName );
    ISecurityHelper securityHelper = service.getSecurityHelper();
    when( securityHelper.getAuthentication( Mockito.any( IPentahoSession.class ), eq( true ) ) ).thenReturn( userAuthentication );

    // act
    IDomainStatusMessage result = service.installPlugin( "doesNotMatter", "doesNotMatter" );

    // assert
    assertThat( result.getCode(), is( equalTo( BasePluginService.UNAUTHORIZED_ACCESS_ERROR_CODE ) ) );
  }

  /**
   * Tests that when allowed users are defined and the user trying to uninstall is not one of them
   * then the uninstall is denied.
   */
  @Test
  public void testUninstallDeniedUserNotInUsers( ) {
    // arrange
    String userName = "Dennis";
    Collection<String> authorizedUsers = Arrays.asList( "Joseph" , "David" );
    Collection<String> authorizedRoles = Collections.emptyList();
    BaPluginService service = this.createPluginService();

    service.setAuthorizedUsernames( authorizedUsers );
    service.setAuthorizedRoles( authorizedRoles );

    Authentication userAuthentication = this.createMockUserAuthentication( null, userName );
    ISecurityHelper securityHelper = service.getSecurityHelper();
    when( securityHelper.getAuthentication( Mockito.any( IPentahoSession.class ), eq( true ) ) ).thenReturn( userAuthentication );

    // act
    IDomainStatusMessage result = service.uninstallPlugin( "doesNotMatter" );

    // assert
    assertThat( result.getCode(), is( equalTo( BasePluginService.UNAUTHORIZED_ACCESS_ERROR_CODE ) ) );
  }

  //TODO: "(un)install allowed" test cases

  /**
   * Tests that only compatible versions (with ba server) of plugins are returned
   */
  @Test
  public void testGetPluginsOnlyCompatibleVersions( ) {
    // arrange
    BaPluginService service = this.createPluginService();
    service.setServerVersion( "5.2" );

    IPluginVersion compatibleVersion = this.pluginVersionFactory.create();
    compatibleVersion.setMinParentVersion( "1.0" );
    compatibleVersion.setMaxParentVersion( "6.9.99" );

    IPluginVersion notCompatibleVersion = this.pluginVersionFactory.create();
    notCompatibleVersion.setMaxParentVersion( "1.0" );
    notCompatibleVersion.setMinParentVersion( "1.0" );

    Collection<IPluginVersion> versions = new ArrayList<>();
    versions.add( compatibleVersion );
    versions.add( notCompatibleVersion );

    Map<String, IPlugin> plugins = new HashMap<>();
    IPlugin plugin = this.pluginFactory.create();
    String pluginId = "myPlugin";
    plugin.setId( pluginId );
    plugin.setVersions( versions );
    plugin.setType( MarketEntryType.Platform );
    plugins.put(plugin.getId(), plugin );

    IPluginProvider pluginProvider = service.getMetadataPluginsProvider();
    when( pluginProvider.getPlugins() ).thenReturn( plugins );

    // act
    Map<String, IPlugin> actualPlugins = service.getPlugins();

    // assert
    IPlugin actualPlugin = actualPlugins.get( pluginId );
    Collection<IPluginVersion> actualVersions = actualPlugin.getVersions();

    assertThat( actualPlugin, is( equalTo( plugin ) ) );
    assertTrue( actualVersions.contains( compatibleVersion ) );
    assertFalse( actualVersions.contains( notCompatibleVersion ) );
  }

  /**
   * Tests that plugins which are installed (in the system folder) are marked as installed
   */
  @Test
  public void testGetPluginsInstalledPluginsAreIdentified() {
    // arrange
    BaPluginService service = this.createPluginService();
    service.setServerVersion( "5.2" );

    IPluginVersion compatibleVersion = this.pluginVersionFactory.create();
    compatibleVersion.setMinParentVersion( "1.0" );
    compatibleVersion.setMaxParentVersion( "6.9.99" );

    IPlugin installedPlugin = this.pluginFactory.create();
    // plugin name is the same as the plugin folder inside "test-res/pentaho-solutions/system"
    String installedPluginId = "installedPlugin";
    installedPlugin.setId( installedPluginId );
    installedPlugin.setInstalled( false );
    installedPlugin.setType( MarketEntryType.Platform );
    // have at least one compatible version so its not filtered out
    installedPlugin.getVersions().add( compatibleVersion );

    IPlugin notInstalledPlugin = this.pluginFactory.create();
    String notInstalledPluginId = "notInstalledPlugin";
    notInstalledPlugin.setId( notInstalledPluginId );
    notInstalledPlugin.setInstalled( false );
    notInstalledPlugin.setType( MarketEntryType.Platform );
    // have at least one compatible version so its not filtered out
    notInstalledPlugin.getVersions().add( compatibleVersion );

    Map<String, IPlugin> plugins = new HashMap<>();
    plugins.put( installedPlugin.getId(), installedPlugin );
    plugins.put( notInstalledPlugin.getId(), notInstalledPlugin );

    IPluginProvider pluginProvider = service.getMetadataPluginsProvider();
    when( pluginProvider.getPlugins() ).thenReturn( plugins );

    // act
    Map<String, IPlugin> actualPlugins = service.getPlugins();

    // assert
    IPlugin actualInstalledPlugin = actualPlugins.get( installedPluginId );
    IPlugin actualNotInstalledPlugin = actualPlugins.get( notInstalledPluginId );
    assertThat( actualInstalledPlugin, is( notNullValue() ) );
    assertThat( actualNotInstalledPlugin, is( notNullValue() ) );
    assertThat( actualInstalledPlugin.isInstalled(), is( true ) );
    assertThat( actualNotInstalledPlugin.isInstalled(), is( false ) );
  }



  // endregion

}
