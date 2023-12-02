package com.test.bluetooththermalprinter
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.test.bluetooththermalprinter.databinding.ActivityMainBinding
import java.io.InputStream
import java.io.OutputStream

import android.app.AlertDialog
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.print.PrintHelper
import java.io.IOException
import java.nio.charset.Charset
import java.text.DecimalFormat
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var btPermission = false
    private var bluetoothAdapter:BluetoothAdapter? = null
    private var socket:BluetoothSocket? = null
    private var bluetoothDevice:BluetoothDevice? = null
    private var outputstream:OutputStream? = null
    private var inputStream:InputStream? = null
    private var workerThread:Thread? = null
    private lateinit var readbuffer:ByteArray
    private var readBufferPosition = 0

     var stopWorker =false
    private var value = ""
    private val connectionCLass:ConnectionCLass = ConnectionCLass()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "BLuetooth Printer Example Kotlin"
    }


    fun checkPermission(){
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter:  BluetoothAdapter?=bluetoothManager.adapter
        if (bluetoothAdapter ==null){
            // Device doesent Support Bluetooth
        }else{
            if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.S){
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }else{
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)

            }
        }
    }
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){
            isGranted :Boolean ->
        if (isGranted){
            val bluetoothManager:BluetoothManager = getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter:BluetoothAdapter ?= bluetoothManager.adapter
            btPermission = true
            if (bluetoothAdapter?.isEnabled == false){
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                btActivityResultLauncher.launch(enableBtIntent)
            }else{
                btScan()

            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun btScan() {
        val bluetoothManager:BluetoothManager = getSystemService((BluetoothManager::class.java))
        val bluetoothAdapter : BluetoothAdapter? = bluetoothManager.adapter
        val builder = AlertDialog.Builder(this@MainActivity)
        val inflater = layoutInflater
        val dialogView:View = inflater.inflate(R.layout.scan_bt,null)
        builder.setCancelable(false)
        builder.setView(dialogView)
        val btlst = dialogView.findViewById<ListView>(R.id.bt_lst)
        val dialog = builder.create()
        val pairedDevice:Set<BluetoothDevice> = bluetoothAdapter?.bondedDevices as Set<BluetoothDevice>
        val ADAhere:SimpleAdapter
        var data:MutableList<Map<String?,Any?>?>? = null
        data =ArrayList()
        if(pairedDevice.isNotEmpty()){
            val datanum1:MutableMap<String?,Any?> = HashMap()
            datanum1["A"]= ""
            datanum1["A"]= ""
            data.add(datanum1)
            for(device in pairedDevice){
                val datanum:MutableMap<String?,Any?> = HashMap()
                datanum["A"] = device.name
                datanum["A"] = device.name
                data.add(datanum)
            }
            val fromwhere = arrayOf("A")
            val viewswhere = intArrayOf(R.id.ite_name)
            ADAhere = SimpleAdapter(this@MainActivity,data,R.layout.item_list,fromwhere,viewswhere)
            btlst.adapter = ADAhere
            ADAhere.notifyDataSetChanged()
            btlst.onItemClickListener = AdapterView.OnItemClickListener{
                adapterView, view, position, l ->
                val String = ADAhere.getItem((position)) as HashMap<String,String>
                val prnName = String["A"]
                binding.deviceName.setText(prnName)
                connectionCLass.printer_name = prnName.toString()
                dialog.dismiss()

            }
        }else{
            val value = "No Devices Found"
            Toast.makeText(this,value,Toast.LENGTH_LONG).show()
            return
        }
        dialog.show()


    }
    fun beginListenForData(){
//         for tommorow strt with this line please
        try{
            val handler = Handler()
            val delimiter :Byte = 10
            stopWorker = false
            readBufferPosition =0
            readbuffer = ByteArray(1024)
            workerThread = Thread {
                while (!Thread.currentThread().isInterrupted && !stopWorker){
                    try{
                        val bytesAvailable = inputStream!!.available()
                        if (bytesAvailable >0 ){
                            val packetBytes = ByteArray(bytesAvailable)
                            inputStream!!.read(packetBytes)
                            for (i in 0 until bytesAvailable){
                               val b = packetBytes[i]
                               if (b == delimiter){
                                   val encodedBytes = ByteArray(readBufferPosition)
                                   System.arraycopy(readbuffer,0,encodedBytes,0,encodedBytes.size)
                                   val data =String(encodedBytes, Charset.forName("US-ASCII"))
                                   readBufferPosition = 0
                                   handler.post{ Log.d("e",data)}
                               }else{
                                   readbuffer[readBufferPosition++] = b
                               }
                            }
                        }
                    } catch (ex:IOException){
                        stopWorker = true
                    }
                }
            }
            workerThread!!.start()
        } catch (ex:IOException){

        }

    }
    @SuppressLint("MissingPermission", "SuspiciousIndentation")
    fun InitPrinter(){
        var prname: String  = ""
        prname = connectionCLass.printer_name.toString()
        val bluetoothManager : BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        try{
            if (bluetoothAdapter !=null){
                if (!bluetoothAdapter.isEnabled){
                    val enableBluetooth =Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    btActivityResultLauncher.launch(enableBluetooth)
                }
            }
            val pairedDevices = bluetoothAdapter?.bondedDevices

                if (pairedDevices != null) {
                    if (pairedDevices.size >0) {
                        for(device in pairedDevices){
                            if (device.name == prname){
                                bluetoothDevice = device
                                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                                val m =bluetoothDevice!!.javaClass.getMethod(
                                    "createRfcoommSocket",*arrayOf<Class<*>?>(
                                        Int::class.javaPrimitiveType
                                    )
                                )
                                socket = m.invoke(bluetoothDevice,1) as BluetoothSocket
                                bluetoothAdapter?.cancelDiscovery()
                                socket!!.connect()
                                outputstream = socket!!.outputStream
                                inputStream = socket!!.inputStream
                                beginListenForData()
                                break
                             }
                        }
                    }
                }else{
                    value = "NO devices Found"
                    Toast.makeText(this,value,Toast.LENGTH_LONG).show()
                    return
                }

        }catch (ex:java.lang.Exception){
            Toast.makeText(this,"Bluetooth Printer Not Connected",Toast.LENGTH_LONG).show()
            socket = null

        }
    }

    fun print_inv(){
        try{
            var str:String
            var invhdr:String = "Tax Invoice"
            var add:String = "Deccon Tech"
            var mo:String = ""
            var gstin:String = ""
            var billno: String = ""
            var billdt:String = ""
            var tblno:String = ""
            var stw:String = ""
            var msg: String = ""
            var amtwd : String =""

            val wnm ="Self"
            val logName = "Admin"
            var amt = 0.0
            var gst = 0.0
            var gamt = 0.0
            var cmpname:String = "THermal Printer Sample"
            mo = "98000000000"
            gstin ="Gst no"
            billno = "1001"
            billdt = "06-09-2023"
            tblno = "5"

            msg = "Thanks"
            amtwd = "One HUndread FIVE oNLY"
            amt = 100.0
            gst = 5.0
            gamt = 100.00

            val textData = StringBuilder()
            val textData1 = StringBuilder()
            val textData2 = StringBuilder()
            val textData3 = StringBuilder()
            val textData4 = StringBuilder()

            if (invhdr.isNotEmpty()){
                textData.append("""$invhdr""".trimIndent())

            }
            textData.append("""$cmpname""".trimIndent())
            if (mo.isNotEmpty()){
                textData.append("""$mo""".trimIndent())
            }

            if (gstin.isNotEmpty()){
                textData.append("""$gstin""".trimIndent())
            }
            str = ""
            str = String().format("%-14s %17s","Inv$billno","Table:$tblno")
            textData.append("""$str""".trimIndent())
            textData.append("Date Time:$billdt\n")

//             method   = "ALIGN_LEFT"
            textData1.append("---------------------\n")
            textData1.append("""
                Item Description 
            """.trimIndent())

            str = ""
            str = String.format("%-11s %9s %10s", "Qty", "Rate", "Amount")
            textData1.append("""
                $str
            """.trimIndent())
            textData1.append("----------------------------\n")
            val df  = DecimalFormat("0.00")
            var itmname:String
            var rt:String?
            var qty :String
            var amount:String?

            for (i in  0 until 10){
                val price = 10
                itmname = "Item $i"
                rt = df.format(price)
                qty = "1 pc"
                amount = "10.0"
                textData1.append(itmname+"\n")
                str = ""
                str = String.format("%-11s %9s %10s",qty,rt,amount)
                textData1.append(str+"\n")
            }

            textData1.append("----------\n")
            str = ""

            str = String.format("%-9s %-11s %10s", wnm,"Total:",amt)
            textData1.append(str+"\n")
            str = ""
            str = String.format("%-9s %-11s %10s", logName,"Gst:",amt)
            textData1.append(str+"\n")
            str = ""
            str = String.format("%-9s %8s","Total:",gamt)
            textData2.append(str+"\n")
            textData3.append(amtwd+"\n")
            if (msg.isNotEmpty()){
                textData4.append(msg+"\n")
            }
            textData4.append("Android App \n\n\n\n")
            IntentPrint(textData.toString(),textData1.toString(),textData2.toString(),textData3.toString(),textData4.toString())


        }catch (ex: java.lang.Exception){
            value += "$ex\nExcep IntentPrint \n"
            Toast.makeText(this,value,Toast.LENGTH_LONG).show()
        }
    }

    fun IntentPrint(
        textvalue:String,
        textvalue1:String,
        textvalue2:String,
        textvalue3:String,
        textvalue4:String
        ){

        if (connectionCLass.printer_name.trim().isNotEmpty()){
            val buffer = textvalue1.toByteArray()
            val PrintHeader = byteArrayOf(0xAA.toByte(),0x55,2,0)
            PrintHeader[3] = buffer.size.toByte()
            InitPrinter()
            if (PrintHeader.size >128){
                value += "\nValue is more than 128 size \n"
                Toast.makeText(this,value,Toast.LENGTH_LONG).show()

            }else{
                try{
                    if(socket!=null){
                        try{
                            val SP = byteArrayOf(0x1B,0x40)
                            outputstream!!.write(SP)
                            Thread.sleep(1000)
                        }catch (e: InterruptedException ){
                            e.printStackTrace()
                        }

                        val FONT_1X  = byteArrayOf(0x1B,0x21,0x00)
                        outputstream!!.write(FONT_1X)
                        val ALIGN_CENTER = byteArrayOf(0x1B,0x61,1)
                        outputstream!!.write(ALIGN_CENTER)
                        outputstream!!.write(textvalue.toByteArray())

                        val ALIGN_LEFT = byteArrayOf(0x1B,0x61,0)
                        outputstream!!.write(ALIGN_LEFT)
                        outputstream!!.write(textvalue1.toByteArray())

                        val FONT_2X = byteArrayOf(0x1b,0x21,0x30)
                        outputstream!!.write(FONT_2X)
                        outputstream!!.write(textvalue1.toByteArray())
                        outputstream!!.write(FONT_1X)
                        outputstream!!.write(ALIGN_LEFT)
                        outputstream!!.write(textvalue3.toByteArray())
                        outputstream!!.write(ALIGN_CENTER)
                        outputstream!!.write(textvalue4.toByteArray())


                        val FEED_PAPER_AND_CUT = byteArrayOf(0x1D,0x56,66,0x00)
                        outputstream!!.write(FEED_PAPER_AND_CUT)
                        outputstream!!.flush()
                        outputstream!!.close()





                    }
                }catch (ex: java.lang.Exception){
                    Toast.makeText(this,ex.message.toString(),Toast.LENGTH_LONG).show()

                }
            }
        }

    }

    private val btActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ result : ActivityResult ->
        if(result.resultCode == RESULT_OK){
            btScan()
        }

    }

    fun scanBt(view: View){
        checkPermission()
    }

    fun print(view: View){
        if (btPermission){
            print_inv()
        }else{
            checkPermission()
        }
    }



}

