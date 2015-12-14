package _appId_.view
{
	/**
	 * @author SamYStudiO ( contact@samystudio.net )
	 */
	public final class EnumScreen
	{
		/**
		 * Top root screen (StarlingMain)
		 */
		public static const MAIN : String = "main";

		/**
		 *
		 */
		public static const HOME : String = "home";

		/**
		 *
		 */
		public static const OTHER_SCREEN : String = "otherScreen";

		/**
		 * @private
		 */
		public function EnumScreen()
		{
			throw new Error( this + " cannot be instantiated" );
		}
	}
}
