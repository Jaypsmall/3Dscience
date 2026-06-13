package com.example.a3dscience

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a3dscience.model.Element
import com.example.a3dscience.model.Vector3
import com.example.a3dscience.ui.theme._3DscienceTheme
import com.example.a3dscience.viewmodel.MoleculeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _3DscienceTheme {
                val viewModel: MoleculeViewModel = viewModel()
                ScientificApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificApp(viewModel: MoleculeViewModel) {
    val molecule by viewModel.molecule.collectAsState()
    val selectedAtomId by viewModel.selectedAtomId.collectAsState()
    val surfaceMesh by viewModel.surfaceMesh.collectAsState()
    val isWhiteBg by viewModel.isWhiteBackground.collectAsState()
    val isCalculating by viewModel.isCalculating.collectAsState()
    val showMesh by viewModel.showMesh.collectAsState()
    val lang by viewModel.language.collectAsState()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedElement by remember { mutableStateOf(Element.CARBON) }
    var currentBondType by remember { mutableStateOf(com.example.a3dscience.model.BondType.SINGLE) }
    
    // INTERACCIÓN 3D
    var isPanMode by remember { mutableStateOf(false) }
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    var atomMenuOffset by remember { mutableStateOf(Offset(20f, 220f)) }
    var toolMenuOffset by remember { mutableStateOf(Offset(100f, 60f)) }
    
    val projectedAtoms = remember { mutableStateListOf<ProjectedAtom>() }

    val t = @Composable { en: String, es: String -> if (lang == "ES") es else en }

    Surface(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ScientificDrawer(viewModel, scope, drawerState, lang)
            },
            gesturesEnabled = true // SIEMPRE ACTIVADO EL DESLIZAMIENTO LATERAL
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // VISOR 3D
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isWhiteBg) Color(0xFFF5F5F7) else Color(0xFF010101))
                        .pointerInput(isPanMode) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                // IGNORAR SI EL GESTO EMPIEZA EN EL BORDE IZQUIERDO (PARA EL DRAWER)
                                if (centroid.x > 40f) {
                                    zoomScale = (zoomScale * zoom).coerceIn(0.2f, 5f)
                                    if (isPanMode) {
                                        panOffset += pan
                                    } else {
                                        rotationY += pan.x * 0.5f
                                        rotationX += pan.y * 0.5f
                                    }
                                }
                            }
                        }
                        .pointerInput(molecule, rotationX, rotationY, panOffset, zoomScale) {
                            detectTapGestures { offset ->
                                val clicked = projectedAtoms.filter {
                                    val d = sqrt((it.x - offset.x).let { x -> x*x } + (it.y - offset.y).let { y -> y*y })
                                    d < it.radius * 2.5f
                                }.maxByOrNull { it.z }

                                if (clicked != null) {
                                    viewModel.selectAtom(clicked.id, currentBondType)
                                } else {
                                    val scale = 140f * zoomScale
                                    val centerX = size.width / 2 + panOffset.x
                                    val centerY = size.height / 2 + panOffset.y
                                    val rx = (offset.x - centerX) / scale
                                    val ry = (offset.y - centerY) / scale
                                    viewModel.addAtomAt(selectedElement, Vector3(rx, ry, 0f))
                                }
                            }
                        }
                ) {
                    Molecule3DRenderer(
                        molecule = molecule,
                        selectedAtomId = selectedAtomId,
                        surfaceMesh = if (showMesh) surfaceMesh else emptyList(),
                        isWhiteBg = isWhiteBg,
                        rotationX = rotationX,
                        rotationY = rotationY,
                        panOffset = panOffset,
                        zoomScale = zoomScale,
                        projectedAtoms = projectedAtoms
                    )
                }

                // CAJETÍN HERRAMIENTAS
                Box(
                    modifier = Modifier
                        .offset { IntOffset(toolMenuOffset.x.toInt(), toolMenuOffset.y.toInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                toolMenuOffset += dragAmount
                            }
                        }
                ) {
                    Surface(
                        color = (if (isWhiteBg) Color.White else Color(0xFF1A1A1A)).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Menu, "Menu", tint = if (isWhiteBg) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                            }
                            
                            VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 8.dp))

                            IconButton(onClick = { viewModel.undo() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Undo, "Undo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }

                            VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 8.dp))

                            Row(modifier = Modifier.clip(CircleShape).background(if (isWhiteBg) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f))) {
                                IconButton(
                                    onClick = { isPanMode = false },
                                    modifier = Modifier.size(32.dp).background(if (!isPanMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                ) {
                                    Icon(Icons.Default.RotateRight, null, modifier = Modifier.size(16.dp), tint = if (!isPanMode) Color.White else (if (isWhiteBg) Color.Black else Color.White))
                                }
                                IconButton(
                                    onClick = { isPanMode = true },
                                    modifier = Modifier.size(32.dp).background(if (isPanMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                ) {
                                    Icon(Icons.Default.PanTool, null, modifier = Modifier.size(16.dp), tint = if (isPanMode) Color.White else (if (isWhiteBg) Color.Black else Color.White))
                                }
                            }

                            VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 12.dp))
                            
                            Surface(
                                onClick = {
                                    currentBondType = when(currentBondType) {
                                        com.example.a3dscience.model.BondType.SINGLE -> com.example.a3dscience.model.BondType.DOUBLE
                                        com.example.a3dscience.model.BondType.DOUBLE -> com.example.a3dscience.model.BondType.TRIPLE
                                        else -> com.example.a3dscience.model.BondType.SINGLE
                                    }
                                },
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = when(currentBondType) {
                                        com.example.a3dscience.model.BondType.SINGLE -> t("S —", "S —")
                                        com.example.a3dscience.model.BondType.DOUBLE -> t("D =", "D =")
                                        com.example.a3dscience.model.BondType.TRIPLE -> t("T ≡", "T ≡")
                                        else -> t("AR", "AR")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (isWhiteBg) Color.Black else Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // CAJETÍN ÁTOMOS
                Box(
                    modifier = Modifier
                        .offset { IntOffset(atomMenuOffset.x.toInt(), atomMenuOffset.y.toInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                atomMenuOffset += dragAmount
                            }
                        }
                ) {
                    ElementFloatingBar(
                        selectedElement = selectedElement,
                        isWhiteBg = isWhiteBg,
                        onElementSelected = { selectedElement = it }
                    )
                }

                // INFO HUD
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (isCalculating) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(t("CALCULATING FIELD...", "CALCULANDO CAMPO..."), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Surface(
                        color = (if (isWhiteBg) Color.Black else Color.White).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = molecule.atoms.groupBy { it.element.symbol }.map { "${it.key}${it.value.size}" }.joinToString(""),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = if (isWhiteBg) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ElementFloatingBar(selectedElement: Element, isWhiteBg: Boolean, onElementSelected: (Element) -> Unit) {
    Surface(
        color = (if (isWhiteBg) Color.White else Color(0xFF1A1A1A)).copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 16.dp,
        shadowElevation = 12.dp,
        modifier = Modifier.width(54.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Box(modifier = Modifier.size(24.dp, 4.dp).background(Color.Gray.copy(alpha = 0.4f), CircleShape))
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 450.dp)
            ) {
                items(Element.entries) { element ->
                    val isSelected = selectedElement == element
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) element.color.copy(alpha = 0.4f) else Color.Transparent)
                            .pointerInput(element) { detectTapGestures { onElementSelected(element) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = element.color,
                            shape = CircleShape,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    element.symbol,
                                    color = if (element.color == Color.White || element.color == Color(0xFFF0F0F0)) Color.Black else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Molecule3DRenderer(
    molecule: com.example.a3dscience.model.Molecule,
    selectedAtomId: Int?,
    surfaceMesh: List<com.example.a3dscience.model.SurfaceEdge>,
    isWhiteBg: Boolean,
    rotationX: Float,
    rotationY: Float,
    panOffset: Offset,
    zoomScale: Float,
    projectedAtoms: androidx.compose.runtime.snapshots.SnapshotStateList<ProjectedAtom>
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2 + panOffset.x
            val centerY = size.height / 2 + panOffset.y
            val baseScale = 140f * zoomScale

            val cosY = cos(Math.toRadians(rotationY.toDouble())).toFloat()
            val sinY = sin(Math.toRadians(rotationY.toDouble())).toFloat()
            val cosX = cos(Math.toRadians(rotationX.toDouble())).toFloat()
            val sinX = sin(Math.toRadians(rotationX.toDouble())).toFloat()

            // 1. REJILLA
            val gridColor = if (isWhiteBg) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f)
            val step = 80f * zoomScale
            for (i in -15..15) {
                drawLine(gridColor, Offset(0f, centerY + i * step), Offset(size.width, centerY + i * step), 1f)
                drawLine(gridColor, Offset(centerX + i * step, 0f), Offset(centerX + i * step, size.height), 1f)
            }

            // 2. MALLA MEP (LA "TELARAÑA") - MÁS VISIBLE
            surfaceMesh.forEach { edge ->
                val p1 = project(edge.start, cosX, sinX, cosY, sinY, centerX, centerY, baseScale)
                val p2 = project(edge.end, cosX, sinX, cosY, sinY, centerX, centerY, baseScale)
                
                // Color dinámico según la intensidad MEP (Carga)
                val color = when {
                    edge.intensity > 0.4f -> Color.Red.copy(alpha = 0.7f)   // Nucleofílico (Rojo Neón)
                    edge.intensity < -0.2f -> Color.Blue.copy(alpha = 0.7f) // Electrofílico (Azul Neón)
                    else -> Color.Green.copy(alpha = 0.4f)                  // Neutro (Técnico)
                }
                
                // Dibujamos la telaraña reforzada
                drawLine(color, Offset(p1.x, p1.y), Offset(p2.x, p2.y), 2.5f * zoomScale)
                
                // Puntos de energía brillantes
                drawCircle(color.copy(alpha = 0.9f), 4.5f * zoomScale, Offset(p1.x, p1.y))
            }

            // 3. ENLACES (BONDS) CIENTÍFICOS (DIBUJO MULTI-LINEA)
            molecule.bonds.forEach { bond ->
                val a1 = molecule.atoms.find { it.id == bond.atom1Id }
                val a2 = molecule.atoms.find { it.id == bond.atom2Id }
                if (a1 != null && a2 != null) {
                    val p1 = project(a1.position, cosX, sinX, cosY, sinY, centerX, centerY, baseScale)
                    val p2 = project(a2.position, cosX, sinX, cosY, sinY, centerX, centerY, baseScale)
                    
                    val bondColor = if (isWhiteBg) Color(0xFF333333) else Color(0xFFAAAAAA)
                    val baseThickness = 6f * zoomScale
                    
                    // Cálculo de vector perpendicular para líneas paralelas
                    val dx = p2.x - p1.x
                    val dy = p2.y - p1.y
                    val len = sqrt(dx*dx + dy*dy)
                    val nx = -dy / len * (8f * zoomScale)
                    val ny = dx / len * (8f * zoomScale)
                    
                    when(bond.type) {
                        com.example.a3dscience.model.BondType.DOUBLE -> {
                            drawLine(bondColor, Offset(p1.x - nx/2, p1.y - ny/2), Offset(p2.x - nx/2, p2.y - ny/2), baseThickness)
                            drawLine(bondColor, Offset(p1.x + nx/2, p1.y + ny/2), Offset(p2.x + nx/2, p2.y + ny/2), baseThickness)
                        }
                        com.example.a3dscience.model.BondType.TRIPLE -> {
                            drawLine(bondColor, Offset(p1.x, p1.y), Offset(p2.x, p2.y), baseThickness)
                            drawLine(bondColor, Offset(p1.x - nx, p1.y - ny), Offset(p2.x - nx, p2.y - ny), baseThickness)
                            drawLine(bondColor, Offset(p1.x + nx, p1.y + ny), Offset(p2.x + nx, p2.y + ny), baseThickness)
                        }
                        else -> {
                            drawLine(bondColor, Offset(p1.x, p1.y), Offset(p2.x, p2.y), baseThickness * 1.8f)
                        }
                    }
                }
            }

            // 4. ÁTOMOS
            val newProjected = mutableListOf<ProjectedAtom>()
            val sortedAtoms = molecule.atoms.map { atom ->
                val p = project(atom.position, cosX, sinX, cosY, sinY, centerX, centerY, baseScale)
                atom to p
            }.sortedBy { it.second.z }

            sortedAtoms.forEach { (atom, p) ->
                val zScale = (p.z + 20) / 40f 
                val radius = atom.element.radius * 65f * zScale * zoomScale
                newProjected.add(ProjectedAtom(atom.id, p.x, p.y, p.z, radius))

                if (atom.id == selectedAtomId) {
                    drawCircle(Color(0xFFFFD700), radius + 15f * zoomScale, Offset(p.x, p.y), style = Stroke(6f * zoomScale))
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(atom.element.color, atom.element.color.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.7f)),
                        center = Offset(p.x - radius * 0.35f, p.y - radius * 0.35f),
                        radius = radius * 2.0f
                    ),
                    radius = radius,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = radius * 0.4f,
                    center = Offset(p.x - radius * 0.45f, p.y - radius * 0.45f)
                )
            }
            projectedAtoms.clear()
            projectedAtoms.addAll(newProjected)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificDrawer(viewModel: MoleculeViewModel, scope: kotlinx.coroutines.CoroutineScope, drawerState: DrawerState, lang: String) {
    val t = { en: String, es: String -> if (lang == "ES") es else en }
    
    ModalDrawerSheet(
        modifier = Modifier.width(320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxHeight()) {
            Text(t("LAB CONTROL", "CONTROL DE LABORATORIO"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(32.dp))
            DrawerActionItem(Icons.Default.Science, t("LOAD DMT (C12H16N2)", "CARGAR DMT (C12H16N2)")) {
                viewModel.loadDMT()
                scope.launch { drawerState.close() }
            }
            DrawerActionItem(Icons.Default.Cloud, t("GENERATE ISOSURFACE", "GENERAR ISOSUPERFICIE")) {
                viewModel.generateSurfaceMesh()
                scope.launch { drawerState.close() }
            }
            DrawerActionItem(Icons.Default.Palette, t("TOGGLE DARK/LIGHT", "CAMBIAR FONDO")) {
                viewModel.toggleBackground()
            }
            DrawerActionItem(Icons.Default.Grid4x4, t("MESH VISIBILITY", "VISIBILIDAD MALLA")) {
                viewModel.toggleMesh()
            }
            DrawerActionItem(Icons.Default.Language, t("LANGUAGE: ENGLISH", "IDIOMA: ESPAÑOL")) {
                viewModel.toggleLanguage()
            }
            Spacer(Modifier.weight(1f))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            DrawerActionItem(Icons.Default.Delete, t("CLEAR SCENE", "LIMPIAR ESCENA"), tint = Color.Red) {
                viewModel.clearMolecule()
                scope.launch { drawerState.close() }
            }
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icono_3dscience),
                    contentDescription = "Logo",
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun DrawerActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, null, tint = tint) },
        label = { Text(label, fontWeight = FontWeight.Bold, color = tint, fontSize = 13.sp) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp),
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
    )
}

data class ProjectedAtom(val id: Int, val x: Float, val y: Float, val z: Float, val radius: Float)
data class Point3D(val x: Float, val y: Float, val z: Float)

fun project(
    pos: com.example.a3dscience.model.Vector3,
    cosX: Float, sinX: Float, cosY: Float, sinY: Float,
    centerX: Float, centerY: Float, scale: Float
): Point3D {
    val x1 = pos.x * cosY - pos.z * sinY
    val z1 = pos.x * sinY + pos.z * cosY
    val y2 = pos.y * cosX - z1 * sinX
    val z2 = pos.y * sinX + z1 * cosX
    return Point3D(centerX + x1 * scale, centerY + y2 * scale, z2)
}
