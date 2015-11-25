package _appId_.utils
{
	import _appId_.actors.STAGE;

	import flash.display.StageOrientation;
	import flash.events.Event;
	import flash.geom.Matrix;

	import org.osflash.signals.Signal;

	/**
	 * @author SamYStudiO ( contact@samystudio.net )
	 */
	public final class OrientationManager
	{
		/**
		 *
		 */
		private static var __instance : OrientationManager;

		/**
		 *
		 */
		public static function getInstance() : OrientationManager
		{
			if( __instance == null ) __instance = new OrientationManager( new Singleton() );

			return __instance;
		}

		/**
		 *
		 */
		private var _stageOrientation : String;

		/**
		 *
		 */
		public function get stageOrientation() : String
		{
			return _stageOrientation;
		}

		public function set stageOrientation( s : String ) : void
		{
			STAGE.setOrientation( s );
		}

		/**
		 *
		 */
		private var _deviceOrientation : String;

		/**
		 *
		 */
		public function get deviceOrientation() : String
		{
			return _deviceOrientation;
		}

		/**
		 *
		 */
		public function get isStagePortrait() : Boolean
		{
			return DeviceInfos.isDesktop() ? STAGE.stageWidth < STAGE.stageHeight : STAGE.fullScreenWidth < STAGE.fullScreenHeight;
		}

		/**
		 *
		 */
		public function get isStageLandscape() : Boolean
		{
			return DeviceInfos.isDesktop() ? STAGE.stageWidth > STAGE.stageHeight : STAGE.fullScreenWidth > STAGE.fullScreenHeight;
		}

		/**
		 *
		 */
		public function get isDevicePortrait() : Boolean
		{
			return ( _defautOrientationIsPortrait && ( _deviceOrientation == StageOrientation.DEFAULT || _deviceOrientation == StageOrientation.UPSIDE_DOWN ) ) || ( !_defautOrientationIsPortrait && ( _deviceOrientation == StageOrientation.ROTATED_LEFT || _deviceOrientation == StageOrientation.ROTATED_RIGHT ) );
		}

		/**
		 *
		 */
		public function get isDeviceLandscape() : Boolean
		{
			return !isDevicePortrait;
		}

		/**
		 *
		 */
		private var _defautOrientationIsPortrait : Boolean;

		/**
		 *
		 */
		public function get defautOrientationIsPortrait() : Boolean
		{
			return _defautOrientationIsPortrait;
		}

		/**
		 *
		 */
		public function get defautOrientationIsLandscape() : Boolean
		{
			return !_defautOrientationIsPortrait;
		}

		/**
		 * @private
		 */
		protected var _deviceVSStageMatrix : Matrix;

		/**
		 *
		 */
		public function get deviceVSStageMatrix() : Matrix
		{
			return _deviceVSStageMatrix.clone();
		}

		/**
		 * @private
		 */
		protected var _stageOrientationChanged : Signal = new Signal();

		/**
		 *
		 */
		public function get stageOrientationChanged() : Signal
		{
			return _stageOrientationChanged;
		}

		/**
		 * @private
		 */
		protected var _deviceOrientationChanged : Signal = new Signal();

		/**
		 *
		 */
		public function get deviceOrientationChanged() : Signal
		{
			return _deviceOrientationChanged;
		}

		/**
		 * @private
		 */
		public function OrientationManager( singleton : Singleton )
		{
			if( singleton == null ) throw new Error( this + " Singleton instance can only be accessed through getInstance method" );

			_stageOrientation = STAGE.orientation == StageOrientation.UNKNOWN ? StageOrientation.DEFAULT : STAGE.orientation;

			var orientationIsDefaultUpDown : Boolean = _stageOrientation == StageOrientation.DEFAULT || _stageOrientation == StageOrientation.UPSIDE_DOWN;
			var orientationIsRightLeft : Boolean = _stageOrientation == StageOrientation.ROTATED_LEFT || _stageOrientation == StageOrientation.ROTATED_RIGHT;

			_defautOrientationIsPortrait = orientationIsDefaultUpDown && isStagePortrait || orientationIsRightLeft && isStageLandscape;
			_deviceOrientation = STAGE.deviceOrientation == StageOrientation.UNKNOWN ? ( _stageOrientation == StageOrientation.ROTATED_RIGHT ? StageOrientation.ROTATED_LEFT : _stageOrientation == StageOrientation.ROTATED_LEFT ? StageOrientation.ROTATED_RIGHT : _stageOrientation ) : STAGE.deviceOrientation;
			_deviceVSStageMatrix = new Matrix();
		}

		/**
		 *
		 */
		public function startListeningForChange() : void
		{
			STAGE.addEventListener( Event.ENTER_FRAME , _monitorOrientation );
		}

		/**
		 *
		 */
		public function stopListeningForChange() : void
		{
			STAGE.removeEventListener( Event.ENTER_FRAME , _monitorOrientation );
		}

		/**
		 *
		 */
		public function updateStageOrientationFromDeviceOrientation() : void
		{
			stageOrientation = _deviceOrientation == StageOrientation.ROTATED_LEFT ? StageOrientation.ROTATED_RIGHT : _deviceOrientation == StageOrientation.ROTATED_RIGHT ? StageOrientation.ROTATED_LEFT : _deviceOrientation;
		}

		/**
		 *
		 */
		private function _monitorOrientation( e : Event = null ) : void
		{
			var newStageOrientation : String = STAGE.orientation == StageOrientation.UNKNOWN ? _stageOrientation : STAGE.orientation;
			var stageWidth : Number = DeviceInfos.isDesktop() ? STAGE.stageWidth : STAGE.fullScreenWidth;
			var stageHeight : Number = DeviceInfos.isDesktop() ? STAGE.stageHeight : STAGE.fullScreenHeight;
			var stageIsPortrait : Boolean = ( _defautOrientationIsPortrait && ( newStageOrientation == StageOrientation.DEFAULT || newStageOrientation == StageOrientation.UPSIDE_DOWN ) ) || ( !_defautOrientationIsPortrait && ( newStageOrientation == StageOrientation.ROTATED_LEFT || newStageOrientation == StageOrientation.ROTATED_RIGHT ) );
			var stageOrientationChanged : Boolean = _stageOrientation != newStageOrientation && ( ( stageIsPortrait && stageWidth < stageHeight ) || ( !stageIsPortrait && stageWidth >= stageHeight ) );

			var newDeviceOrientation : String = STAGE.deviceOrientation == StageOrientation.UNKNOWN ? _deviceOrientation : STAGE.deviceOrientation;
			var deviceOrientationChanged : Boolean = _deviceOrientation != newDeviceOrientation;

			_stageOrientation = newStageOrientation;
			_deviceOrientation = newDeviceOrientation;

			if( stageOrientationChanged || deviceOrientationChanged )
			{
				var deviceOrientationFromStage : String = _stageOrientation == StageOrientation.ROTATED_LEFT ? StageOrientation.ROTATED_RIGHT : _stageOrientation == StageOrientation.ROTATED_RIGHT ? StageOrientation.ROTATED_LEFT : _stageOrientation;
				var offset : uint = _orientations.indexOf( deviceOrientationFromStage );
				var currentIndex : int = _orientations.indexOf( _deviceOrientation );

				currentIndex -= offset;

				if( currentIndex > _orientations.length - 1 ) currentIndex -= _orientations.length;
				if( currentIndex < 0 ) currentIndex += _orientations.length;

				var orientation : String = _orientations[ currentIndex ];

				_deviceVSStageMatrix.identity();

				if( orientation == StageOrientation.ROTATED_LEFT )
				{
					_deviceVSStageMatrix.rotate( Math.PI / 2 );
					_deviceVSStageMatrix.translate( 1 , 0 );
				}
				else if( orientation == StageOrientation.ROTATED_RIGHT )
				{
					_deviceVSStageMatrix.rotate( -Math.PI / 2 );
					_deviceVSStageMatrix.translate( 0 , 1 );
				}
				else if( orientation == StageOrientation.UPSIDE_DOWN )
				{
					_deviceVSStageMatrix.rotate( Math.PI );
					_deviceVSStageMatrix.translate( 1 , 1 );
				}
			}

			if( stageOrientationChanged ) this.stageOrientationChanged.dispatch();

			if( deviceOrientationChanged ) this.deviceOrientationChanged.dispatch();
		}

		/**
		 *
		 */
		private const _orientations : Array = [ StageOrientation.DEFAULT , StageOrientation.ROTATED_LEFT , StageOrientation.UPSIDE_DOWN , StageOrientation.ROTATED_RIGHT ];
	}
}

internal class Singleton
{
}
