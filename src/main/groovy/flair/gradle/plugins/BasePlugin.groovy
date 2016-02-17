package flair.gradle.plugins

import flair.gradle.dependencies.Config
import flair.gradle.dependencies.Sdk
import flair.gradle.extensions.Extension
import flair.gradle.extensions.FlairProperty
import flair.gradle.extensions.IExtensionManager
import flair.gradle.extensions.factories.FlairExtensionFactory
import flair.gradle.extensions.factories.IExtensionFactory
import flair.gradle.structures.CommonStructure
import flair.gradle.structures.IStructure
import flair.gradle.structures.VariantStructure
import flair.gradle.tasks.GenerateFontsClass
import flair.gradle.tasks.GenerateResourcesClass
import flair.gradle.tasks.TaskDefinition
import flair.gradle.variants.Variant
import flair.gradle.variants.Variant.NamingTypes
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware

/**
 * @author SamYStudiO ( contact@samystudio.net )
 */
class BasePlugin extends AbstractPlugin implements IExtensionPlugin , IStructurePlugin , IConfigurationPlugin
{
	private List<IPlugin> plugins = new ArrayList<IPlugin>( )

	private IExtensionManager flair

	public BasePlugin()
	{
	}

	@Override
	public void apply( Project project )
	{
		super.apply( project )

		project.plugins.whenPluginAdded {

			if( it instanceof IPlugin ) plugins.add( it )

			if( it instanceof IExtensionPlugin )
			{
				it.extensionFactory.create( it == this ? project : project.extensions.getByName( Extension.FLAIR.name ) as ExtensionAware , project )

				if( it == this )
				{
					flair = project.extensions.getByName( Extension.FLAIR.name ) as IExtensionManager
				}
			}

			if( it instanceof IConfigurationPlugin )
			{
				// can't do after evaluation since evaluation may need them for dependencies
				createConfigurations( it.configurations )
			}
		}

		checkLocalProperties( )

		project.afterEvaluate {

			if( isReady( ) )
			{
				createStructures( )
				createVariantTasks( )
				createHandlerTasks( )
				createGeneratedTasks( )
			}
		}
	}

	@Override
	public IExtensionFactory getExtensionFactory()
	{
		return new FlairExtensionFactory( )
	}

	@Override
	public List<Config> getConfigurations()
	{
		return Config.DEFAULTS
	}

	@Override
	public List<IStructure> getStructures()
	{
		List<IStructure> list = new ArrayList<IStructure>( )

		list.add( new CommonStructure( ) )
		list.add( new VariantStructure( ) )

		return list
	}

	@Override
	protected void addTasks()
	{
		project.tasks.create( TaskDefinition.CLEAN.name , TaskDefinition.CLEAN.type )
		project.tasks.create( TaskDefinition.ASDOC.name , TaskDefinition.ASDOC.type )
	}

	private boolean isReady()
	{
		boolean hasPackageName = flair.getFlairProperty( FlairProperty.PACKAGE_NAME ) as boolean
		boolean hasValidSdk = new Sdk( project ).isAirSdk( )

		if( !hasValidSdk )
		{
			throw new Exception( "Cannot find AIR SDK home, set a valid AIR SDK home from your local.properties file under project root" )
		}
		if( !hasPackageName )
		{
			throw new Exception( String.format( "Missing flair property packageName, add it to your build.gradle file :%nflair {%npackageName \"com.hello.world\"%n}" ) )
		}

		return true
	}

	private void checkLocalProperties()
	{
		File file = project.file( "${ project.rootDir.path }/local.properties" )

		if( !file.exists( ) )
		{
			file.createNewFile( )
			file.write( String.format( "## This file should *NOT* be checked into Version Control Systems,%n# as it contains information specific to your local configuration.%n#%n# Location of the Adobe AIR SDK. This is only used by Gradle.%n# For customization when using a Version Control System, please read the%n# header note.%nsdk.dir=" ) )
		}
	}

	private void createConfigurations( List<Config> list )
	{
		list.each { conf ->

			project.configurations.create( conf.name ) {

				if( conf.files )
				{
					conf.files.each {
						project.dependencies.add( conf.name , project.files( "${ flair.getFlairProperty( FlairProperty.MODULE_NAME ) }/${ it }" ) )
					}
				}

				if( conf.fileTree )
				{
					Map<String , String> map = conf.fileTree.clone( ) as Map<String , String>

					map.each {

						if( it.key == "dir" ) it.value = "${ flair.getFlairProperty( flair.gradle.extensions.FlairProperty.MODULE_NAME ) }/${ it.value }"
					}

					project.dependencies.add( conf.name , project.fileTree( map ) )
				}
			}
		}
	}

	private void createStructures()
	{
		String moduleName = flair.getFlairProperty( FlairProperty.MODULE_NAME )
		String packageName = flair.getFlairProperty( flair.gradle.extensions.FlairProperty.PACKAGE_NAME )

		if( !moduleName || !packageName ) return

		String tempDir = System.getProperty( "java.io.tmpdir" )
		String scaffoldTempDir = "${ tempDir }/scaffold"

		project.copy {
			from project.zipTree( getClass( ).getProtectionDomain( ).getCodeSource( ).getLocation( ).getPath( ) )
			into tempDir

			include "scaffold/**"
			exclude "**/.gitkeep"
		}

		GenerateFontsClass.template = project.file( "${ scaffoldTempDir }/src/main/generated/Fonts.as" ).text
		GenerateResourcesClass.template = project.file( "${ scaffoldTempDir }/src/main/generated/R.as" ).text

		project.plugins.each {
			if( it instanceof IStructurePlugin )
			{
				it.structures.each { structure -> structure.create( project , project.file( scaffoldTempDir ) ) }
			}
		}

		project.file( scaffoldTempDir ).deleteDir( )
	}

	private void createVariantTasks()
	{
		project.plugins.each { plugin ->
			if( plugin instanceof IVariantTaskPlugin )
			{
				plugin.variantTaskFactories.each { factory ->

					boolean hasVariant = false

					flair.getPlatformVariants( plugin instanceof IPlatformPlugin ? plugin.platform : null ).each { variant ->

						factory.create( project , variant )

						hasVariant = true
					}

					if( !hasVariant && plugin instanceof IPlatformPlugin )
					{
						factory.create( project , new Variant( project , plugin.platform ) )
					}
				}
			}
		}
	}

	private void createHandlerTasks()
	{
		List<String> listAssemble = new ArrayList<String>( )
		List<String> listCompile = new ArrayList<String>( )
		List<String> listPackage = new ArrayList<String>( )

		String assemble
		String compile
		String pack

		Task t

		flair.allActivePlatformVariants.each {

			assemble = TaskDefinition.ASSEMBLE.name + it.getNameWithType( NamingTypes.CAPITALIZE )
			compile = TaskDefinition.COMPILE.name + it.getNameWithType( NamingTypes.CAPITALIZE )
			pack = TaskDefinition.PACKAGE.name + it.getNameWithType( NamingTypes.CAPITALIZE )

			if( project.tasks.findByName( assemble ) ) listAssemble.add( assemble )
			if( project.tasks.findByName( compile ) ) listCompile.add( compile )
			if( project.tasks.findByName( pack ) ) listPackage.add( pack )
		}

		if( listAssemble.size( ) > 1 && PluginManager.getCurrentPlatforms( project ).size( ) > 1 )
		{
			t = project.tasks.findByName( TaskDefinition.ASSEMBLE.name + "All" )
			if( !t )
			{
				t = project.tasks.create( TaskDefinition.ASSEMBLE.name + "All" )
				t.group = TaskDefinition.ASSEMBLE.group.name
				t.dependsOn listAssemble
			}
		}

		if( listCompile.size( ) > 1 && PluginManager.getCurrentPlatforms( project ).size( ) > 1 )
		{
			t = project.tasks.findByName( TaskDefinition.COMPILE.name + "All" )
			if( !t )
			{
				t = project.tasks.create( TaskDefinition.COMPILE.name + "All" )
				t.group = TaskDefinition.COMPILE.group.name
				t.dependsOn listCompile
			}
		}

		if( listPackage.size( ) > 1 && PluginManager.getCurrentPlatforms( project ).size( ) > 1 )
		{
			t = project.tasks.findByName( TaskDefinition.PACKAGE.name + "All" )
			if( !t )
			{
				t = project.tasks.create( TaskDefinition.PACKAGE.name + "All" )
				t.group = TaskDefinition.PACKAGE.group.name
				t.dependsOn listPackage
			}
		}

		PluginManager.getCurrentPlatforms( project ).each {

			listAssemble = new ArrayList<String>( )
			listCompile = new ArrayList<String>( )
			listPackage = new ArrayList<String>( )

			flair.getPlatformVariants( it ).each {
				assemble = TaskDefinition.ASSEMBLE.name + it.getNameWithType( NamingTypes.CAPITALIZE )
				compile = TaskDefinition.COMPILE.name + it.getNameWithType( NamingTypes.CAPITALIZE )
				pack = TaskDefinition.PACKAGE.name + it.getNameWithType( NamingTypes.CAPITALIZE )

				if( project.tasks.findByName( assemble ) ) listAssemble.add( assemble )
				if( project.tasks.findByName( compile ) ) listCompile.add( compile )
				if( project.tasks.findByName( pack ) ) listPackage.add( pack )
			}

			if( listAssemble.size( ) > 1 )
			{
				t = project.tasks.findByName( TaskDefinition.ASSEMBLE.name + "All" + it.name.capitalize( ) )
				if( !t )
				{
					t = project.tasks.create( TaskDefinition.ASSEMBLE.name + "All" + it.name.capitalize( ) )
					t.group = TaskDefinition.ASSEMBLE.group.name
					t.dependsOn listAssemble
				}
			}

			if( listCompile.size( ) > 1 )
			{

				t = project.tasks.findByName( TaskDefinition.COMPILE.name + "All" + it.name.capitalize( ) )
				if( !t )
				{
					t = project.tasks.create( TaskDefinition.COMPILE.name + "All" + it.name.capitalize( ) )
					t.group = TaskDefinition.COMPILE.group.name
					t.dependsOn listCompile
				}
			}

			if( listPackage.size( ) > 1 )
			{

				t = project.tasks.findByName( TaskDefinition.PACKAGE.name + "All" + it.name.capitalize( ) )
				if( !t )
				{
					t = project.tasks.create( TaskDefinition.PACKAGE.name + "All" + it.name.capitalize( ) )
					t.group = TaskDefinition.PACKAGE.group.name
					t.dependsOn listPackage
				}
			}
		}

		List<String> buildTypes = flair.allActivePlatformBuildTypes

		if( buildTypes.size( ) > 1 )
		{
			buildTypes.each { type ->

				listAssemble = new ArrayList<String>( )
				listCompile = new ArrayList<String>( )
				listPackage = new ArrayList<String>( )

				flair.allActivePlatformVariants.each {
					assemble = TaskDefinition.ASSEMBLE.name + it.getNameWithType( NamingTypes.CAPITALIZE )
					compile = TaskDefinition.COMPILE.name + it.getNameWithType( NamingTypes.CAPITALIZE )
					pack = TaskDefinition.PACKAGE.name + it.getNameWithType( NamingTypes.CAPITALIZE )

					if( it.buildType == type && project.tasks.findByName( assemble ) ) listAssemble.add( assemble )
					if( it.buildType == type && project.tasks.findByName( compile ) ) listCompile.add( compile )
					if( it.buildType == type && project.tasks.findByName( pack ) ) listPackage.add( pack )
				}

				if( listAssemble.size( ) > 1 )
				{

					t = project.tasks.findByName( TaskDefinition.ASSEMBLE.name + "All" + type.capitalize( ) )
					if( !t )
					{
						t = project.tasks.create( TaskDefinition.ASSEMBLE.name + "All" + type.capitalize( ) )
						t.group = TaskDefinition.ASSEMBLE.group.name
						t.dependsOn listAssemble
					}
				}

				if( listCompile.size( ) > 1 )
				{

					t = project.tasks.findByName( TaskDefinition.COMPILE.name + "All" + type.capitalize( ) )
					if( !t )
					{
						t = project.tasks.create( TaskDefinition.COMPILE.name + "All" + type.capitalize( ) )
						t.group = TaskDefinition.COMPILE.group.name
						t.dependsOn listCompile
					}
				}

				if( listPackage.size( ) > 1 )
				{

					t = project.tasks.findByName( TaskDefinition.PACKAGE.name + "All" + type.capitalize( ) )
					if( !t )
					{
						t = project.tasks.create( TaskDefinition.PACKAGE.name + "All" + type.capitalize( ) )
						t.group = TaskDefinition.PACKAGE.group.name
						t.dependsOn listPackage
					}
				}
			}
		}
	}

	private createGeneratedTasks()
	{
		GenerateResourcesClass resourcesTask = project.tasks.create( TaskDefinition.GENERATE_RESOURCES_CLASS.name , TaskDefinition.GENERATE_RESOURCES_CLASS.type ) as GenerateResourcesClass
		resourcesTask.findInputAndOutputFiles( )

		GenerateFontsClass fontsTask = project.tasks.create( TaskDefinition.GENERATE_FONTS_CLASS.name , TaskDefinition.GENERATE_FONTS_CLASS.type ) as GenerateFontsClass
		fontsTask.findInputAndOutputFiles( )
	}
}
