package flair.gradle.tasks.factory

import flair.gradle.platforms.Platform
import org.gradle.api.Project

/**
 * @author SamYStudiO ( contact@samystudio.net )
 */
interface VariantTaskFactory<T> extends TaskFactory<T>
{
	public T create( Project project , String prefix , boolean singlePlatform , List<String> dependencies )

	public T create( Project project , String prefix , Platform platform , boolean singlePlatform , String productFlavor , String buildType , List<String> dependencies )
}