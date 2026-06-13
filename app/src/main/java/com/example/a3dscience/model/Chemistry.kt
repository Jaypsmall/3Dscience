package com.example.a3dscience.model

import androidx.compose.ui.graphics.Color

enum class Element(
    val symbol: String,
    val atomicNumber: Int,
    val color: Color,
    val radius: Float, // Van der Waals radius in Angstroms
    val electronegativity: Float // Pauling scale
) {
    HYDROGEN("H", 1, Color(0xFFF0F0F0), 1.20f, 2.20f),
    CARBON("C", 6, Color(0xFF333333), 1.70f, 2.55f),
    NITROGEN("N", 7, Color(0xFF3050F8), 1.55f, 3.04f),
    OXYGEN("O", 8, Color(0xFFFF0D0D), 1.52f, 3.44f),
    FLUORINE("F", 9, Color(0xFF90E050), 1.47f, 3.98f),
    CHLORINE("Cl", 17, Color(0xFF1FF01F), 1.75f, 3.16f),
    PHOSPHORUS("P", 15, Color(0xFFFF8000), 1.80f, 2.19f),
    SULFUR("S", 16, Color(0xFFFFFF30), 1.80f, 2.58f);
}

data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    fun length() = kotlin.math.sqrt(x * x + y * y + z * z)
}

data class Atom(
    val id: Int,
    val element: Element,
    val position: Vector3
)

enum class BondType {
    SINGLE, DOUBLE, TRIPLE, AROMATIC
}

data class Bond(
    val atom1Id: Int,
    val atom2Id: Int,
    val type: BondType = BondType.SINGLE
)

data class Molecule(
    val name: String,
    val atoms: List<Atom> = emptyList(),
    val bonds: List<Bond> = emptyList()
)

data class SurfaceEdge(val start: Vector3, val end: Vector3, val intensity: Float)
