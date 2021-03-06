package net.geeksempire.experimental.demonstration.DynamicFeatures

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.splitinstall.*
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.google.android.play.core.tasks.Task
import com.google.firebase.FirebaseApp
import kotlinx.android.synthetic.main.dynamic_feature.*
import net.geeksempire.experimental.demonstration.BaseConfigurations
import net.geeksempire.experimental.demonstration.BuildConfig
import net.geeksempire.experimental.demonstration.R
import net.geeksempire.experimental.demonstration.Utils.Functions.FunctionsClass

class DynamicFeaturesUpdateActivity : AppCompatActivity() {

    lateinit var functionsClass: FunctionsClass

    lateinit var splitInstallManager: SplitInstallManager
    lateinit var splitInstallStateUpdatedListener: SplitInstallStateUpdatedListener

    lateinit var appUpdateManager: AppUpdateManager
    lateinit var installStateUpdatedListener: InstallStateUpdatedListener

    val SPLIT_REQUEST_CODE = 111
    val IN_APP_UPDATE_REQUEST = 333

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(getApplicationContext());
        setContentView(R.layout.dynamic_feature)

        functionsClass = FunctionsClass(applicationContext)

        //Installing Module
        splitInstallStateUpdatedListener =
            SplitInstallStateUpdatedListener { splitInstallSessionState ->
                when (splitInstallSessionState.status()) {
                    SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                        //Play Store.
                        println("*** Split Confirmation ***")
                        splitInstallManager.startConfirmationDialogForResult(
                            splitInstallSessionState,
                            this@DynamicFeaturesUpdateActivity,
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

                                    val width = bitmap.width
                                    val height = bitmap.height
                                    val outputBitmap =
                                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

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

                                    dynamicImage.setImageBitmap((outputBitmap))
                                }
                            }, 1000)
                        }
                        when (splitInstallSessionState.moduleNames()[0]) {
                            BaseConfigurations.dynamicModule -> {
                                Intent().setClassName(
                                    BuildConfig.APPLICATION_ID,
                                    BaseConfigurations.dynamicClassName
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

        //Updating Module
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
                    && updateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        updateInfo,
                        AppUpdateType.FLEXIBLE,
                        this@DynamicFeaturesUpdateActivity,
                        IN_APP_UPDATE_REQUEST
                    )
                }

            }
            .addOnFailureListener {
                println("*** Exception Error ${it} ***")
            }

        if (installedModule.contains(BaseConfigurations.dynamicModule)) {
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
            if (installedModule.contains(BaseConfigurations.dynamicModule)) {
                Intent().setClassName(
                    BuildConfig.APPLICATION_ID,
                    BaseConfigurations.dynamicClassName
                ).also {
                    it.putExtra("DATA_TO_DYNAMIC", "ALREADY INSTALLED")
                    startActivity(it)
                }
            } else {
                val splitInstallRequest: SplitInstallRequest =
                    SplitInstallRequest
                        .newBuilder()
                        .addModule(BaseConfigurations.dynamicModule)
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
                BaseConfigurations.dynamicModule
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
                        AppUpdateType.FLEXIBLE,
                        this@DynamicFeaturesUpdateActivity,
                        IN_APP_UPDATE_REQUEST
                    )
                }

                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    showCompleteConfirmation()
                }
            }
    }

    override fun onPause() {
        super.onPause()
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

            } else if (resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
                println("*** RESULT_IN_APP_UPDATE_FAILED ***")
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
