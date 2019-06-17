package net.geeksempire.experimental.demonstration

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.splitinstall.*
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.google.android.play.core.tasks.Task
import com.google.firebase.FirebaseApp
import kotlinx.android.synthetic.main.app_main_view.*


class AppViewActivity : BaseConfigurations() {

    lateinit var splitInstallManager: SplitInstallManager
    lateinit var splitInstallStateUpdatedListener: SplitInstallStateUpdatedListener

    lateinit var appUpdateManager: AppUpdateManager
    lateinit var installStateUpdatedListener: InstallStateUpdatedListener

    val SPLIT_REQUEST_CODE = 111
    val IN_APP_UPDATE_REQUEST = 333

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(getApplicationContext());
        setContentView(R.layout.app_main_view)

        splitInstallStateUpdatedListener = SplitInstallStateUpdatedListener { splitInstallSessionState ->
            when (splitInstallSessionState.status()) {
                SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                    //Play Store.
                    println("*** Split Confirmation ***")
                    splitInstallManager.startConfirmationDialogForResult(
                        splitInstallSessionState,
                        this@AppViewActivity,
                        SPLIT_REQUEST_CODE
                    )
                }
                SplitInstallSessionStatus.DOWNLOADING -> {
                    //Play Store.
                    println("*** Split Downloading ***")
                }
                SplitInstallSessionStatus.DOWNLOADED -> {

                }
                SplitInstallSessionStatus.INSTALLING -> {
                    println("*** Split Installing ***")
                }
                SplitInstallSessionStatus.INSTALLED -> {
                    println("*** Module Installed: ${splitInstallSessionState.moduleNames()[0]}")
                    Handler().run {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            // Updates app info with new split information making split artifacts available to the
                            // app on subsequent requests.
                            SplitInstallHelper.updateAppInfo(applicationContext);
                        }

                        Handler().postDelayed({
                            runOnUiThread {
                                val assetManager = assets
                                val inputStream = assetManager.open("dynamic_image.png")
                                val bitmap = BitmapFactory.decodeStream(inputStream)

                                val width = bitmap.getWidth()
                                val height = bitmap.getHeight()
                                val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                                val path = Path()
                                path.addCircle(
                                    (width / 2).toFloat(),
                                    (height / 2).toFloat(),
                                    (dynamicImage.width).toFloat(),
                                    Path.Direction.CCW
                                )

                                val canvas = Canvas(outputBitmap)
                                canvas.clipPath(path)
                                canvas.drawBitmap(bitmap, 0f, 0f, null)

                                dynamicImage.setImageBitmap(outputBitmap)
                            }
                        }, 1000)
                    }
                    when (splitInstallSessionState.moduleNames()[0]) {
                        dynamicModule -> {
                            Intent().setClassName(
                                BuildConfig.APPLICATION_ID,
                                dynamicClassName
                            )
                                .also {
                                    it.putExtra("DATA_TO_DYNAMIC", "AFTER INSTALLED")
                                    startActivity(it)
                                }
                        }
                    }
                    splitInstallManager.unregisterListener(splitInstallStateUpdatedListener)
                }
                SplitInstallSessionStatus.FAILED -> {

                }
            }
        }
        splitInstallManager = SplitInstallManagerFactory.create(applicationContext)
        splitInstallManager.registerListener(splitInstallStateUpdatedListener)
        val installedModule = splitInstallManager.installedModules
        installedModule.forEach {
            println("*** Installed Modules: " + it + " ***")
        }

        installStateUpdatedListener = InstallStateUpdatedListener {
            when (it.installStatus()) {
                InstallStatus.REQUIRES_UI_INTENT -> {
                    println("*** UPDATE UI Intent ***")
                }
                InstallStatus.DOWNLOADING -> {
                    println("*** UPDATE Downloading ***")
                }
                InstallStatus.DOWNLOADED -> {
                    println("*** UPDATE Downloaded ***")

                    showCompleteConfirmation()
                }
                InstallStatus.INSTALLING -> {
                    println("*** UPDATE Installing ***")
                }
                InstallStatus.INSTALLED -> {
                    println("*** UPDATE Installed ***")

                    appUpdateManager.unregisterListener(installStateUpdatedListener)
                }
                InstallStatus.CANCELED -> {
                    println("*** UPDATE Canceled ***")
                }
                InstallStatus.FAILED -> {
                    println("*** UPDATE Failed ***")
                }
            }
        }
        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        appUpdateManager.registerListener(installStateUpdatedListener)
        val appUpdateInfo: Task<AppUpdateInfo> = appUpdateManager.appUpdateInfo
        appUpdateInfo
            .addOnSuccessListener { updateInfo ->
                println("*** ${updateInfo.updateAvailability()} --- ${updateInfo.availableVersionCode()} ***")
                if (updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && updateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        updateInfo,
                        AppUpdateType.IMMEDIATE,
                        this@AppViewActivity,
                        IN_APP_UPDATE_REQUEST
                    )
                }

            }
            .addOnFailureListener {
                println("*** Exception Error ${it} ***")
            }

        if (installedModule.contains(dynamicModule)) {
            Handler().postDelayed({
                runOnUiThread {
                    val assetManager = assets
                    val inputStream = assetManager.open("dynamic_image.png")
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    val width = bitmap.getWidth()
                    val height = bitmap.getHeight()
                    val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                    val path = Path()
                    path.addCircle(
                        (width / 2).toFloat(),
                        (height / 2).toFloat(),
                        (dynamicImage.width).toFloat(),
                        Path.Direction.CCW
                    )

                    val canvas = Canvas(outputBitmap)
                    canvas.clipPath(path)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)

                    dynamicImage.setImageBitmap(outputBitmap)
                }
            }, 1000)
        }

        dynamicFeature.setOnClickListener {
            if (installedModule.contains(dynamicModule)) {
                Intent().setClassName(
                    BuildConfig.APPLICATION_ID,
                    dynamicClassName
                ).also {
                    it.putExtra("DATA_TO_DYNAMIC", "ALREADY INSTALLED")
                    startActivity(it)
                }
            } else {
                val splitInstallRequest: SplitInstallRequest =
                    SplitInstallRequest
                        .newBuilder()
                        .addModule(dynamicModule)
                        .build()

                splitInstallManager
                    .startInstall(splitInstallRequest)
                    .addOnSuccessListener {
                        println("Module Installed Successfully")
                    }
                    .addOnFailureListener {
                        println("Exception Error:" + it)
                    }
            }
        }
        dynamicFeature.setOnLongClickListener {

            val moduleToUninstall = listOf<String>(
                dynamicModule
            )
            splitInstallManager.deferredUninstall(moduleToUninstall).addOnSuccessListener {
                println("Module Uninstalled Successfully")

                splitInstallManager.installedModules.forEach {
                    println("*** Installed Modules After Uninstallation: " + it + " ***")
                }
            }.addOnFailureListener {
                println("Exception Error:" + it)
            }

            return@setOnLongClickListener true
        }
    }

    override fun onStart() {
        super.onStart()

        dynamicImage.setOnClickListener {
            //            startActivity(Intent(applicationContext, LoadAds::class.java))
        }

        /************************************Fling*********************************************/
        val flingAnimationX: FlingAnimation by lazy(LazyThreadSafetyMode.NONE) {
            FlingAnimation(dynamicImage, DynamicAnimation.X).setFriction(1.1f)
        }

        val flingAnimationY: FlingAnimation by lazy(LazyThreadSafetyMode.NONE) {
            FlingAnimation(dynamicImage, DynamicAnimation.Y).setFriction(1.1f)
        }

        val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {

                flingAnimationX.setStartVelocity(velocityX)
                flingAnimationY.setStartVelocity(velocityY)

                flingAnimationX.start()
                flingAnimationY.start()

                return false
            }
        }

        val gestureDetector = GestureDetector(applicationContext, gestureListener)

        var dX: Float = 0f
        var dY: Float = 0f

        dynamicImage.setOnTouchListener { view, event ->
            gestureDetector.onTouchEvent(event)

            when (event.getAction()) {

                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                }

                MotionEvent.ACTION_MOVE -> {
                    if ((event.rawX + dX) > 0 && (event.rawY + dY) > 0) {
                        view.animate()
                            .x(event.rawX + dX)
                            .y(event.rawY + dY)
                            .setDuration(0)
                            .start()
                    }

                }
            }


            return@setOnTouchListener false
        }

        dynamicImage.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                flingAnimationX
                    .setMinValue(0f)
                    .setMaxValue((resources.displayMetrics.widthPixels - dynamicImage.width).toFloat())
                flingAnimationY
                    .setMinValue(0f)
                    .setMaxValue((resources.displayMetrics.heightPixels - dynamicImage.height).toFloat())
                dynamicImage.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })


        /************************************Spring*********************************************/
        val springForce: SpringForce by lazy(LazyThreadSafetyMode.NONE) {
            SpringForce(0f).apply {
                stiffness = SpringForce.STIFFNESS_MEDIUM
                dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
            }
        }

        val springAnimationTranslationX: SpringAnimation by lazy(LazyThreadSafetyMode.NONE) {
            SpringAnimation(dynamicFeature, DynamicAnimation.TRANSLATION_X).setSpring(springForce)
        }

        val springAnimationTranslationY: SpringAnimation by lazy(LazyThreadSafetyMode.NONE) {
            SpringAnimation(dynamicFeature, DynamicAnimation.TRANSLATION_Y).setSpring(springForce)
        }

        var xDiffInTouchPointAndViewTopLeftCorner: Float = 0f
        var yDiffInTouchPointAndViewTopLeftCorner: Float = 0f

        dynamicFeature.setOnTouchListener { view, motionEvent ->

            when (motionEvent?.action) {

                MotionEvent.ACTION_DOWN -> {
                    xDiffInTouchPointAndViewTopLeftCorner = motionEvent.rawX - view.x
                    yDiffInTouchPointAndViewTopLeftCorner = motionEvent.rawY - view.y

                    springAnimationTranslationX.cancel()
                    springAnimationTranslationY.cancel()
                }

                MotionEvent.ACTION_MOVE -> {
                    dynamicFeature.x = motionEvent.rawX - xDiffInTouchPointAndViewTopLeftCorner
                    dynamicFeature.y = motionEvent.rawY - yDiffInTouchPointAndViewTopLeftCorner
                }

                MotionEvent.ACTION_UP -> {
                    springAnimationTranslationX.start()
                    springAnimationTranslationY.start()
                }
            }

            true
        }
    }

    override fun onResume() {
        super.onResume()

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this@AppViewActivity,
                        IN_APP_UPDATE_REQUEST
                    )
                }

                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    showCompleteConfirmation()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SPLIT_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                println("*** Split Downloading Canceled ***")
            } else if (resultCode == RESULT_OK) {
                println("*** Split Downloaded Successfully ***")
            }
        } else if (requestCode == IN_APP_UPDATE_REQUEST) {
            if (resultCode == RESULT_CANCELED) {


            } else if (resultCode == RESULT_OK) {

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun showCompleteConfirmation() {
        val snackbar = Snackbar.make(
            findViewById<ConstraintLayout>(R.id.mainView),
            "An Update has Just Been Installed.",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.setAction("Complete It!") { view -> appUpdateManager.completeUpdate() }
        snackbar.setActionTextColor(getColor(android.R.color.holo_blue_bright))
        snackbar.show()
    }
}