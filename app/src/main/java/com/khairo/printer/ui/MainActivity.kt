package com.khairo.printer.ui


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.android.volley.Response
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.khairo.async.*
import com.khairo.coroutines.CoroutinesEscPosPrint
import com.khairo.coroutines.CoroutinesEscPosPrinter
import com.khairo.escposprinter.EscPosPrinter
import com.khairo.escposprinter.connection.DeviceConnection
import com.khairo.escposprinter.connection.tcp.TcpConnection
import com.khairo.escposprinter.exceptions.EscPosBarcodeException
import com.khairo.escposprinter.exceptions.EscPosConnectionException
import com.khairo.escposprinter.exceptions.EscPosEncodingException
import com.khairo.escposprinter.exceptions.EscPosParserException
import com.khairo.escposprinter.textparser.PrinterTextParserImg
import com.khairo.printer.R
import com.khairo.printer.brokers.VolleyBroker
import com.khairo.printer.databinding.ActivityMainBinding
import com.khairo.printer.models.ImpresoraObject
import com.khairo.printer.models.detalleComanda
import com.khairo.printer.models.encabezadoComanda
import com.khairo.printer.models.printerObject
import com.khairo.printer.utils.printViaWifi
import kotlinx.coroutines.*
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var printer: CoroutinesEscPosPrinter? = null
    lateinit var volleyBroker: VolleyBroker
    private var encabezados : List<encabezadoComanda> = listOf()
    private var adrs : List<String> = listOf()
    private var detalles : List<detalleComanda> = listOf()
    private var exceptions: ArrayList<ArrayList<String>> = arrayListOf()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        volleyBroker = VolleyBroker(this.applicationContext)

        var loop = false
        super.onCreate(savedInstanceState)
        //here is date pattern
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:SS")
        var t = Timer()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.apply {

            buttonTest.setOnClickListener {
                val textInputLayout = findViewById<TextInputEditText>(R.id.id_store)
                val strTitle: Editable? = textInputLayout.text
                volleyBroker.requestQueue.add(VolleyBroker.getRequest("getPrinters?idstore="+strTitle.toString(),
                    { response ->
                        val jsonPrintObjectString = "${response}"
                        val collectionTypePrintObject: Type = object :
                            TypeToken<List<ImpresoraObject?>?>() {}.type
                        val listPrintObject: List<ImpresoraObject> =
                            Gson().fromJson(
                                jsonPrintObjectString,
                                collectionTypePrintObject
                            ) as List<ImpresoraObject>
                        GlobalScope.launch(Dispatchers.Default) {
                            for (printObject in listPrintObject) {
                                printTcp(
                                    "[C]<b><font size='big'>TEST</font></b>\n",
                                    printObject.ip,
                                    "9100",
                                    "-1",
                                    printObject.nombre
                                ) {

                                }

                            }
                        }
                    },
                    {

                        Log.d("ERROR EN ENCABEZADOS", it.toString())
                    }))
            }


            buttonTcpStop.setOnClickListener {
                buttonTcp.isEnabled = true
                buttonTcp.isClickable = true
                println("myHandler: IM STOPPING!")
                loop = false
                t.cancel()
            }


            buttonTcp.setOnClickListener {
                t = Timer() //This is new
                buttonTcp.isEnabled = false
                buttonTcp.isClickable = false
                t.scheduleAtFixedRate(
                    object : TimerTask() {
                        override fun run() {
                            //Called each time when 1000 milliseconds (1 second) (the period parameter)

                            val textInputLayout = findViewById<TextInputEditText>(R.id.id_store)
                            val strTitle: Editable? = textInputLayout.text


                            Log.d("IDSTORE", strTitle.toString())
                            loop = true
                            Log.d("TEXTO TO","myHandler: IM starting!")


                            // Define a Runnable to execute printStuff() and post it to the Handler
                            val mainScope = CoroutineScope(Dispatchers.Main)
                            mainScope.launch(Dispatchers.Main) {
                                var myCallback: (() -> Unit)? = null
                                myCallback = {
                                }
                                printStuff(strTitle.toString(),myCallback)
                            }
                        }
                    },  //Set how long before to start calling the TimerTask (in milliseconds)
                    0,  //Set the amount of time between each execution (in milliseconds)
                    90000
                )
            }

            /*
            buttonTcp.setOnClickListener {
                val textInputLayout = findViewById<TextInputEditText>(R.id.id_store)
                val strTitle: Editable? = textInputLayout.text


                Log.d("IDSTORE", strTitle.toString())
                loop = true
                buttonTcp.isEnabled = false
                buttonTcp.isClickable = false
                Log.d("TEXTO TO","myHandler: IM starting!")


                // Define a Runnable to execute printStuff() and post it to the Handler
                val mainScope = CoroutineScope(Dispatchers.Main)
                mainScope.launch(Dispatchers.Main) {
                    var myCallback: (() -> Unit)? = null
                    myCallback = {
                        mainScope.launch(Dispatchers.Main) {
                            //here is date
                            latestPrintDate.text = LocalDateTime.now().format(formatter)
                            Log.d("TEXTO TO", "FINISHED 5 SECOND LOOP")
                            delay(10000L)
                            if(loop){
                                myCallback?.let { it1 -> printStuff(strTitle.toString(),it1) }
                            }
                        }
                    }
                    printStuff(strTitle.toString(),myCallback)
                }
            }*/
        }
    }

    suspend fun printStuff(id_store:String,callback: () -> Unit) {
        volleyBroker.requestQueue.add(VolleyBroker.getRequest("getPrintData?idstore="+id_store+"&impresa=0",
            { response ->
                val jsonPrintObjectString = "${response}"
                val collectionTypePrintObject: Type = object :
                    TypeToken<List<printerObject?>?>() {}.type
                val listPrintObject: List<printerObject> =
                    Gson().fromJson(
                        jsonPrintObjectString,
                        collectionTypePrintObject
                    ) as List<printerObject>

                Log.d("ITEMS TO PRINT", jsonPrintObjectString)

                var counter = 0 // initialize counter
                GlobalScope.launch(Dispatchers.Default) {
                    if(listPrintObject.isEmpty()){
                        callback.invoke()
                    }
                    for (printObject in listPrintObject) {
                        delay(1500)
                        Log.d(
                            "TEXTO TO",
                            printObject.textToPrint + "IS PRINTING"
                        )

                        printTcp(
                            printObject.textToPrint,
                            printObject.ip,
                            printObject.port,
                            printObject.idbill,
                            printObject.adr
                        ) {

                            if(exceptions.isEmpty()){

                                volleyBroker.requestQueue.add(

                                    com.khairo.escposprinter.brokers.VolleyBroker.getRequest("actualizar_estado_impresion_adr?idbill=" + printObject.idbill + "&NOMBRE_ADR=" + printObject.adr + "&respuestaimpresion=0&mensajeerror=EXITO_IMPRESION",
                                        Response.Listener<String> { response ->

                                            volleyBroker.requestQueue.add(VolleyBroker.getRequest(
                                                "actualizar_estado_impresion_bill?idbill=" + printObject.idbill,
                                                Response.Listener<String> { response ->


                                                    Log.d(
                                                        "TEXTO TO",
                                                        printObject.textToPrint + "WAS UPDATED"
                                                    )


                                                    counter++ // increment counter
                                                    if (counter == listPrintObject.size) {
                                                        callback.invoke() // call callback when loop is done
                                                    }


                                                },
                                                Response.ErrorListener {

                                                    volleyBroker.requestQueue.add(
                                                        com.khairo.escposprinter.brokers.VolleyBroker.getRequest(
                                                            "actualizar_estado_impresion_adr?idbill=" + printObject.idbill + "&NOMBRE_ADR=" + printObject.adr + "&respuestaimpresion=1&mensajeerror=error_Impresora",
                                                            Response.Listener<String> { response ->

                                                                Log.d(
                                                                    "ERROR EN UPDATE",
                                                                    it.toString()
                                                                )
                                                            },
                                                            Response.ErrorListener {
                                                                Log.d(
                                                                    "ERROR EN FAILED UP ADR",
                                                                    it.toString()
                                                                )
                                                            }
                                                        ))
                                                }
                                            ))
                                        },
                                        Response.ErrorListener {
                                            volleyBroker.requestQueue.add(
                                                com.khairo.escposprinter.brokers.VolleyBroker.getRequest(
                                                    "actualizar_estado_impresion_adr?idbill=" + printObject.idbill + "&NOMBRE_ADR=" + printObject.adr + "&respuestaimpresion=1&mensajeerror=error_Impresora",
                                                    Response.Listener<String> { response ->

                                                        Log.d(
                                                            "ERROR EN UPDATE",
                                                            it.toString()
                                                        )
                                                    },
                                                    Response.ErrorListener {
                                                        Log.d(
                                                            "ERROR EN FAILED UP ADR",
                                                            it.toString()
                                                        )
                                                    }
                                                ))
                                        }
                                    ))
                            }
                            else {
                                for (i in 0..exceptions.size - 1) {

                                    if (exceptions[i][0] == printObject.adr && exceptions[i][1] == printObject.idbill) {

                                        volleyBroker.requestQueue.add(
                                            com.khairo.escposprinter.brokers.VolleyBroker.getRequest(
                                                "actualizar_estado_impresion_adr?idbill=" + printObject.idbill + "&NOMBRE_ADR=" + printObject.adr + "&respuestaimpresion=1&mensajeerror=error_Impresora",
                                                Response.Listener<String> { response ->

                                                    Log.d(
                                                        "ERROR EN UPDATE",
                                                        "Error al IMPRIMIR"
                                                    )
                                                },
                                                Response.ErrorListener {
                                                    Log.d(
                                                        "ERROR EN FAILED UP ADR",
                                                        it.toString()
                                                    )
                                                }
                                            ))
                                    } else {

                                        volleyBroker.requestQueue.add(

                                            com.khairo.escposprinter.brokers.VolleyBroker.getRequest(
                                                "actualizar_estado_impresion_adr?idbill=" + printObject.idbill + "&NOMBRE_ADR=" + printObject.adr + "&respuestaimpresion=0&mensajeerror=EXITO_IMPRESION",
                                                Response.Listener<String> { response ->

                                                    volleyBroker.requestQueue.add(VolleyBroker.getRequest(
                                                        "actualizar_estado_impresion_bill?idbill=" + printObject.idbill,
                                                        Response.Listener<String> { response ->


                                                            Log.d(
                                                                "TEXTO TO",
                                                                printObject.textToPrint + "WAS UPDATED"
                                                            )

                                                            counter++ // increment counter
                                                            if (counter == listPrintObject.size) {
                                                                callback.invoke() // call callback when loop is done
                                                            }


                                                        },
                                                        Response.ErrorListener {

                                                            volleyBroker.requestQueue.add(
                                                                com.khairo.escposprinter.brokers.VolleyBroker.getRequest(
                                                                    "actualizar_estado_impresion_adr?idbill=" + printObject.idbill + "&NOMBRE_ADR=" + printObject.adr + "&respuestaimpresion=1&mensajeerror=error_Impresora",
                                                                    Response.Listener<String> { response ->

                                                                        Log.d(
                                                                            "ERROR EN UPDATE",
                                                                            it.toString()
                                                                        )
                                                                    },
                                                                    Response.ErrorListener {
                                                                        Log.d(
                                                                            "ERROR EN FAILED UP ADR",
                                                                            it.toString()
                                                                        )
                                                                    }
                                                                ))
                                                        }
                                                    ))
                                                },
                                                Response.ErrorListener {
                                                    volleyBroker.requestQueue.add(
                                                        com.khairo.escposprinter.brokers.VolleyBroker.getRequest(
                                                            "actualizar_estado_impresion_adr?idbill=" + printObject.idbill + "&NOMBRE_ADR=" + printObject.adr + "&respuestaimpresion=1&mensajeerror=error_Impresora",
                                                            Response.Listener<String> { response ->

                                                                Log.d(
                                                                    "ERROR EN UPDATE",
                                                                    it.toString()
                                                                )
                                                            },
                                                            Response.ErrorListener {
                                                                Log.d(
                                                                    "ERROR EN FAILED UP ADR",
                                                                    it.toString()
                                                                )
                                                            }
                                                        ))
                                                }
                                            ))
                                    }

                                    if (i == exceptions.size - 1) {
                                        exceptions = arrayListOf()
                                    }


                                }
                            }

                        }
                    }




                }
            },
            {

                Log.d("ERROR EN ENCABEZADOS", it.toString())
                callback.invoke()
            }
        ))
    }



    /*==============================================================================================
    ===================================ESC/POS PRINTER PART=========================================
    ==============================================================================================*/
    /**
     * Synchronous printing
     */
    @SuppressLint("SimpleDateFormat")
    fun printIt(printerConnection: DeviceConnection?) {
        AsyncTask.execute {
            try {
                val format = SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss")
                val printer = EscPosPrinter(
                    printerConnection,
                    203,
                    48f,
                    32
                )
                printer
                    .printFormattedText(
                        "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(
                            printer,
                            applicationContext.resources.getDrawableForDensity(
                                R.drawable.logo,
                                DisplayMetrics.DENSITY_MEDIUM
                            )
                        ) + "</img>\n" +
                                "[L]\n" +
                                "[C]<u><font size='big'>ORDER N°045</font></u>\n" +
                                "[C]<font size='small'>" + format.format(Date()) + "</font>\n" +
                                "[L]\n" +
                                "[C]=================="+"Pong!"+"==============\n" +
                                "[L]\n" +
                                "[L]<b>BEAUTIFUL SHIRT</b>[R]9.99e\n" +
                                "[L]  + Size : S\n" +
                                "[L]\n" +
                                "[L]<b>AWESOME HAT</b>[R]24.99e\n" +
                                "[L]  + Size : 57/58\n" +
                                "[L]\n" +
                                "[C]--------------------------------\n" +
                                "[R]TOTAL PRICE :[R]34.98e\n" +
                                "[R]TAX :[R]4.23e\n" +
                                "[L]\n" +
                                "[C]================================\n" +
                                "[L]\n" +
                                "[L]<font size='tall'>Customer :</font>\n" +
                                "[L]Raymond DUPONT\n" +
                                "[L]5 rue des girafes\n" +
                                "[L]31547 PERPETES\n" +
                                "[L]Tel : +33801201456\n" +
                                "[L]\n" +
                                "[C]<barcode type='128' height='10'>83125478455134567890</barcode>\n" +
                                "[C]<qrcode size='20'>http://www.developpeur-web.khairo.com/</qrcode>" +
                                "[L]\n" +
                                "[L]\n" +
                                "[L]\n" +
                                "[L]\n" +
                                "[L]\n" +
                                "[L]\n"
                    )
            } catch (e: EscPosConnectionException) {
                e.printStackTrace()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Broken connection")
                    .setMessage(e.message)
                    .show()
            } catch (e: EscPosParserException) {
                e.printStackTrace()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Invalid formatted text")
                    .setMessage(e.message)
                    .show()
            } catch (e: EscPosEncodingException) {
                e.printStackTrace()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Bad selected encoding")
                    .setMessage(e.message)
                    .show()
            } catch (e: EscPosBarcodeException) {
                e.printStackTrace()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Invalid barcode")
                    .setMessage(e.message)
                    .show()
            }
        }
    }

    /**
     * Asynchronous printing
     */
    @SuppressLint("SimpleDateFormat")
    fun getAsyncEscPosPrinter(printerConnection: DeviceConnection?): AsyncEscPosPrinter {
        val format = SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss")
        val printer = AsyncEscPosPrinter(printerConnection!!, 203, 48f, 32)
        return printer.setTextToPrint(
            "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(
                printer,
                this.applicationContext.resources.getDrawableForDensity(
                    R.drawable.logo,
                    DisplayMetrics.DENSITY_MEDIUM
                )
            ) + "</img>\n" +
                    "[L]\n" +
                    "[C]<u><font size='big'>ORDER N°045</font></u>\n" +
                    "[L]\n" +
                    "[C]<u type='double'>" + format.format(Date()) + "</u>\n" +
                    "[C]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]<b>BEAUTIFUL SHIRT</b>[R]9.99e\n" +
                    "[L]  + Size : S\n" +
                    "[L]\n" +
                    "[L]<b>AWESOME HAT</b>[R]24.99e\n" +
                    "[L]  + Size : 57/58\n" +
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[R]TOTAL PRICE :[R]34.98e\n" +
                    "[R]TAX :[R]4.23e\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]<u><font color='bg-black' size='tall'>Customer :</font></u>\n" +
                    "[L]Raymond DUPONT\n" +
                    "[L]5 rue des girafes\n" +
                    "[L]31547 PERPETES\n" +
                    "[L]Tel : +33801201456\n" +
                    "\n" +
                    "[C]<barcode type='128' height='10'>83125478455134567890</barcode>\n" +
                    "[L]\n" +
                    "[C]<qrcode size='20'>http://www.developpeur-web.khairo.com/</qrcode>\n" +
                    "[L]\n" +
                    "[L]\n" +
                    "[L]\n" +
                    "[L]\n" +
                    "[L]\n" +
                    "[L]\n"
        )
    }

    /*==============================================================================================
    =========================================TCP PART===============================================
    ==============================================================================================*/

    private suspend fun printTcp(textToPrint:String,ip:String,port:String,id_bill:String,adr:String,callback: () -> Unit) {
        try {
           // /*
            var tcpConnector = TcpConnection(
                ip,
                port.toInt(),
                adr,
                id_bill
            )
            printer =
                CoroutinesEscPosPrinter(
                    tcpConnector.apply { connect(this@MainActivity) }, 203, 60f, 48
                )

            CoroutinesEscPosPrint(this).execute(
                printViaWifi(
                    printer!!,
                    textToPrint,
                )
            ).apply { printer = null
                      var tmpExceptions = tcpConnector.getExceptions()
                      for(i in 0..tmpExceptions.size-1){
                          exceptions.add(tmpExceptions[i])
                      }
                      callback.invoke()
                    }

            // */

            //callback.invoke()

        } catch (e: Exception) {
            Log.d("ERROR ",e.toString())

            e.printStackTrace()
        }
    }
}

interface VolleyListener {
    fun requestFinished(exsitance: Boolean)
}