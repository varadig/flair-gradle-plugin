package flair.gradle.tasks.variantFactories

import flair.gradle.tasks.LaunchAdl
import flair.gradle.tasks.Tasks
import flair.gradle.variants.Variant
import org.gradle.api.Project

/**
 * @author SamYStudiO ( contact@samystudio.net )
 */
public class LaunchAdlTaskFactory implements IVariantTaskFactory<LaunchAdl>
{
	public LaunchAdl create( Project project , Variant variant )
	{
		String name = Tasks.LAUNCH_ADL.name + variant.getNameWithType( Variant.NamingTypes.CAPITALIZE )

		LaunchAdl t = project.tasks.findByName( name ) as LaunchAdl

		if( !t ) t = project.tasks.create( name , LaunchAdl )

		t.group = Tasks.LAUNCH_ADL.group.name
		t.variant = variant
		t.dependsOn project.tasks.getByName( Tasks.COMPILE.name + variant.getNameWithType( Variant.NamingTypes.CAPITALIZE ) ).name

		return t
	}
}