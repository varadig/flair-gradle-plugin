package flair.gradle.extensions

import flair.gradle.dependencies.Config
import flair.gradle.variants.Platform
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * @author SamYStudiO ( contact@samystudio.net )
 */
public class PlatformContainerExtension extends AbstractPlatformExtension implements IPlatformContainerExtension
{
	private NamedDomainObjectContainer<IVariantExtension> productFlavors

	private NamedDomainObjectContainer<IVariantExtension> buildTypes

	private List<String> flavorDimensions

	public PlatformContainerExtension( String name , Project project , Platform platform )
	{
		super( name , project , platform )

		productFlavors = project.container( VariantExtension ) {
			new VariantExtension( it , project , platform )
		}

		buildTypes = project.container( VariantExtension ) {
			new VariantExtension( it , project , platform )
		}

		productFlavors.whenObjectAdded {
			updateConfigurations( )
		}

		buildTypes.whenObjectAdded {
			updateConfigurations( )
		}
	}

	public void productFlavors( Action<? super NamedDomainObjectContainer<IVariantExtension>> action )
	{
		action.execute( productFlavors )
	}

	public void buildTypes( Action<? super NamedDomainObjectContainer<IVariantExtension>> action )
	{
		action.execute( buildTypes )
	}

	@Override
	public List<String> getFlavorDimensions()
	{
		return flavorDimensions
	}

	@Override
	public void flavorDimensions( List<String> flavorDimensions )
	{
		this.flavorDimensions = flavorDimensions
	}

	@Override
	public void flavorDimensions( String... flavorDimensions )
	{
		this.flavorDimensions = flavorDimensions
	}

	@Override
	public NamedDomainObjectContainer<IVariantExtension> getProductFlavors()
	{
		return productFlavors
	}

	@Override
	public NamedDomainObjectContainer<IVariantExtension> getBuildTypes()
	{
		return buildTypes
	}

	@Override
	public IVariantExtension getProductFlavor( String name )
	{
		return productFlavors.findByName( name ) ? productFlavors.getByName( name ) : new VariantExtension( name , project , platform )
	}

	@Override
	public IVariantExtension getBuildType( String name )
	{
		return buildTypes.findByName( name ) ? buildTypes.getByName( name ) : new VariantExtension( name , project , platform )
	}

	protected updateConfigurations()
	{
		Config.DEFAULTS.each { conf ->

			extensionManager.allActivePlatformVariants.each {

				it.directoriesCapitalized.each { directory ->

					String s = directory + conf.name.capitalize( )

					if( directory != "main" && !project.configurations.findByName( s ) )
					{
						Configuration c = project.configurations.create( s )

						switch( conf )
						{
							case Config.SOURCE:
								project.dependencies.add( c.name , project.files( "${ extensionManager.getFlairProperty( FlairProperty.MODULE_NAME ) }/src/${ name }/actionscript" ) )
								project.dependencies.add( c.name , project.files( "${ extensionManager.getFlairProperty( FlairProperty.MODULE_NAME ) }/src/${ name }/fonts" ) )
								break

							case Config.PACKAGE:
								project.dependencies.add( c.name , project.files( "${ extensionManager.getFlairProperty( FlairProperty.MODULE_NAME ) }/src/${ name }/assets" ) )
								break

							default: break
						}
					}
				}
			}
		}
	}
}
