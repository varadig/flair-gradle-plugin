package flair.gradle.extensions

import flair.gradle.variants.Platform
import org.gradle.api.Project

/**
 * @author SamYStudiO ( contact@samystudio.net )
 */
public class VariantExtension extends AbstractPlatformExtension implements IVariantExtension
{
	private String dimension

	public VariantExtension( String name , Project project , Platform platform )
	{
		super( name , project , platform )
	}

	public String getDimension()
	{
		return dimension
	}

	public void dimension( String dimension )
	{
		this.dimension = dimension
	}
}
