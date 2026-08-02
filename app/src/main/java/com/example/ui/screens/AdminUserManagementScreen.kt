package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehicleEntity
import com.example.ui.TrackingViewModel

@Composable
fun AdminUserManagementScreen(viewModel: TrackingViewModel) {
    val context = LocalContext.current
    val allVehicles by viewModel.allVehicles.collectAsState()
    val activeVehicle by viewModel.activeVehicle.collectAsState()
    val isGoogleSheetsSyncEnabled by viewModel.isGoogleSheetsSyncEnabled.collectAsState()
    val googleSheetsUrl by viewModel.googleSheetsUrl.collectAsState()

    var isAdminUnlocked by remember { mutableStateOf(false) }
    var adminPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var vehicleToEdit by remember { mutableStateOf<VehicleEntity?>(null) }
    var vehicleToDelete by remember { mutableStateOf<VehicleEntity?>(null) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showGoogleSheetsUrlDialog by remember { mutableStateOf(false) }

    val filteredVehicles = allVehicles.filter { v ->
        v.name.contains(searchQuery, ignoreCase = true) ||
        v.licensePlate.contains(searchQuery, ignoreCase = true) ||
        v.driverName.contains(searchQuery, ignoreCase = true)
    }

    // Unlocked Admin View vs Locked Protection Screen
    if (!isAdminUnlocked) {
        // Locked Screen asking for Admin Passcode / PIN
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF6750A4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "🔒 ระบบหลังบ้านสำหรับ Admin",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ผู้ใช้ทั่วไป (User) ไม่สามารถแก้ไขข้อมูลได้ กรุณากรอกรหัส Admin PIN เพื่อปลดล็อกเข้าสู่ระบบจัดการ:",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = {
                            adminPinInput = it
                            pinError = false
                        },
                        label = { Text("กรอกรหัส Admin PIN") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF38BDF8)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = pinError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "❌ รหัส PIN ไม่ถูกต้อง! กรุณาลองใหม่อีกครั้ง",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (adminPinInput == "0511") {
                                isAdminUnlocked = true
                                Toast.makeText(context, "🔓 เข้าสู่ระบบ Admin เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                            } else {
                                pinError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ปลดล็อกเข้าสู่ระบบ Admin", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 หมายเหตุ: ข้อมูลที่ Admin แก้ไขในหน้านี้ จะเชื่อมโยงไปแสดงที่หน้าแอพของผู้ใช้รถ (User) และส่งต่อไปยัง Google Sheet ทันทีแบบ Real-time",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Unlocked Admin Panel
        Scaffold(
            containerColor = Color(0xFF0F172A),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddVehicleDialog = true },
                    containerColor = Color(0xFF6750A4),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("เพิ่มรถ/คนขับ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Header Title Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFF6750A4), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "ระบบหลังบ้าน Admin",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "จัดการข้อมูลผู้ใช้รถ / พนักงานขับรถ & ทะเบียนรถ",
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    isAdminUnlocked = false
                                    adminPinInput = ""
                                    Toast.makeText(context, "🔒 ล็อกระบบ Admin เรียบร้อย", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Lock Admin", tint = Color(0xFFEF4444))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Realtime Connection Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "⚡ เชื่อมโยง Real-time: แก้ไขที่นี่ อัปเดตไปยังหน้า User และส่งไปยัง Google Sheet ทันที",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Google Sheets Link Settings Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF334155),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "🔗 Google Sheets Webhook Connection",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (googleSheetsUrl.isNotBlank()) googleSheetsUrl else "ยังไม่ได้ตั้งค่า Webhook URL",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.syncVehiclesFromCloud { success, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ดึงข้อมูล Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { showGoogleSheetsUrlDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ตั้งค่า Webhook", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ค้นหาด้วยชื่อคนขับ, ชื่อรถ หรือ เลขทะเบียน...", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Vehicle & User List Header
                Text(
                    text = "รายการยานพาหนะและข้อมูลผู้ใช้ (${filteredVehicles.size} รายการ):",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredVehicles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("❌ ไม่พบข้อมูลรถหรือผู้ใช้งานตรงกับที่ค้นหา", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredVehicles, key = { it.id }) { vehicle ->
                            val isSelected = activeVehicle?.id == vehicle.id

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF182232)
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsCar,
                                                contentDescription = null,
                                                tint = if (isSelected) Color(0xFF38BDF8) else Color.Gray,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = vehicle.name,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "ทะเบียน: ${vehicle.licensePlate} • รุ่น/ปี: ${vehicle.modelYear}",
                                                    color = Color.LightGray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(
                                                onClick = { vehicleToEdit = vehicle },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Vehicle",
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { vehicleToDelete = vehicle },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Vehicle",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "👤 ผู้ใช้รถ/คนขับ: ",
                                                    color = Color.Gray,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = vehicle.driverName,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            if (isSelected) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF0284C7)
                                                ) {
                                                    Text(
                                                        text = "กำลังแสดงในแอพ User",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.selectVehicle(vehicle.id)
                                                        Toast.makeText(context, "เลือก ${vehicle.name} (${vehicle.licensePlate}) ให้หน้า User แสดงผลเรียบร้อย", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("เลือกให้ User ใช้งาน", fontSize = 11.sp, color = Color(0xFF38BDF8))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "สถานะ: ${vehicle.status} • GPS: ${String.format("%.4f", vehicle.currentLat)}, ${String.format("%.4f", vehicle.currentLng)}",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )

                                        Button(
                                            onClick = {
                                                viewModel.selectVehicle(vehicle.id)
                                                viewModel.syncCurrentVehicleToGoogleSheetsNow()
                                                Toast.makeText(context, "ส่งข้อมูล ${vehicle.driverName} (${vehicle.licensePlate}) เข้า Google Sheets แล้ว", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ส่งไป Google Sheet", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Vehicle & Driver Dialog
    if (vehicleToEdit != null) {
        val currentV = vehicleToEdit!!
        var editName by remember { mutableStateOf(currentV.name) }
        var editLicensePlate by remember { mutableStateOf(currentV.licensePlate) }
        var editModelYear by remember { mutableStateOf(currentV.modelYear) }
        var editDriverName by remember { mutableStateOf(currentV.driverName) }

        AlertDialog(
            onDismissRequest = { vehicleToEdit = null },
            containerColor = Color.White,
            title = {
                Text("✏️ Admin: แก้ไขข้อมูลผู้ใช้รถ & ยานพาหนะ", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("การแก้ไขนี้จะอัปเดตไปยังหน้าแอพของผู้ใช้รถ (User) และ Google Sheets โดยอัตโนมัติ:", fontSize = 12.sp, color = Color(0xFF49454F))

                    OutlinedTextField(
                        value = editDriverName,
                        onValueChange = { editDriverName = it },
                        label = { Text("👤 ชื่อผู้ใช้รถ / พนักงานขับรถ") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("ชื่อเรียกประจำรถ (เช่น รถสิบล้อ #01)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editLicensePlate,
                        onValueChange = { editLicensePlate = it },
                        label = { Text("เลขทะเบียนรถ") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editModelYear,
                        onValueChange = { editModelYear = it },
                        label = { Text("รุ่น / ปีรถ") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateVehicleDetails(
                            vehicleId = currentV.id,
                            name = editName,
                            licensePlate = editLicensePlate,
                            modelYear = editModelYear,
                            driverName = editDriverName
                        )
                        vehicleToEdit = null
                        Toast.makeText(context, "อัปเดตไปยังหน้า User และ Google Sheets เรียบร้อย!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text("บันทึกการแก้ไข")
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToEdit = null }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    // Delete Vehicle Dialog
    if (vehicleToDelete != null) {
        val targetV = vehicleToDelete!!
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            containerColor = Color.White,
            title = {
                Text("⚠️ ยืนยันลบยานพาหนะ", color = Color(0xFFB3261E), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text("คุณต้องการลบข้อมูล ${targetV.name} (${targetV.licensePlate}) ของผู้ขับ '${targetV.driverName}' ออกจากระบบหรือไม่?", color = Color(0xFF1D1B20), fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVehicle(targetV.id)
                        vehicleToDelete = null
                        Toast.makeText(context, "ลบยานพาหนะเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                ) {
                    Text("ยืนยันลบ")
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        com.example.ui.screens.AddVehicleDialog(
            onDismiss = { showAddVehicleDialog = false },
            onAdd = { name, plate, model, driver ->
                viewModel.addNewVehicle(name, plate, model, driver)
                showAddVehicleDialog = false
                Toast.makeText(context, "เพิ่มยานพาหนะ $plate ($driver) เรียบร้อย!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Google Sheets URL Dialog
    if (showGoogleSheetsUrlDialog) {
        var tempUrl by remember { mutableStateOf(googleSheetsUrl) }
        AlertDialog(
            onDismissRequest = { showGoogleSheetsUrlDialog = false },
            containerColor = Color.White,
            title = {
                Text("🔗 Google Sheets Webhook URL Settings", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text("ใส่ URL ของ Google Apps Script (doPost/doGet Web App) เพื่อรับข้อมูลผู้ใช้รถและพิกัด GPS:", fontSize = 12.sp, color = Color(0xFF49454F))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("Webhook URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateGoogleSheetsUrl(tempUrl)
                        showGoogleSheetsUrlDialog = false
                        Toast.makeText(context, "อัปเดต Google Sheets Webhook URL เรียบร้อย", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text("บันทึก URL")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleSheetsUrlDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}
