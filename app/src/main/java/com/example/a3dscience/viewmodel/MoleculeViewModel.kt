package com.example.a3dscience.viewmodel

import androidx.lifecycle.ViewModel
import com.example.a3dscience.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.sqrt

class MoleculeViewModel : ViewModel() {
    private val _molecule = MutableStateFlow(Molecule("Nueva Molécula"))
    val molecule: StateFlow<Molecule> = _molecule.asStateFlow()

    private val _history = mutableListOf<Molecule>()

    private val _selectedAtomId = MutableStateFlow<Int?>(null)
    val selectedAtomId: StateFlow<Int?> = _selectedAtomId.asStateFlow()

    private val _surfaceMesh = MutableStateFlow<List<SurfaceEdge>>(emptyList())
    val surfaceMesh: StateFlow<List<SurfaceEdge>> = _surfaceMesh.asStateFlow()

    private val _isWhiteBackground = MutableStateFlow(false)
    val isWhiteBackground = _isWhiteBackground.asStateFlow()

    private val _isCalculating = MutableStateFlow(false)
    val isCalculating = _isCalculating.asStateFlow()

    private val _showMesh = MutableStateFlow(true)
    val showMesh = _showMesh.asStateFlow()

    private val _language = MutableStateFlow("EN") // "EN" or "ES"
    val language = _language.asStateFlow()

    private var nextAtomId = 1

    private fun saveToHistory() {
        _history.add(_molecule.value.copy())
        if (_history.size > 50) _history.removeAt(0)
    }

    fun undo() {
        if (_history.isNotEmpty()) {
            _molecule.value = _history.removeAt(_history.size - 1)
        }
    }

    fun toggleBackground() = _isWhiteBackground.update { !it }
    fun toggleMesh() = _showMesh.update { !it }
    fun toggleLanguage() = _language.update { if (it == "EN") "ES" else "EN" }

    fun selectAtom(id: Int?, bondType: BondType = BondType.SINGLE) {
        if (_selectedAtomId.value == id) {
            _selectedAtomId.value = null
        } else if (_selectedAtomId.value != null && id != null) {
            saveToHistory()
            addBond(_selectedAtomId.value!!, id, bondType)
            _selectedAtomId.value = null
        } else {
            _selectedAtomId.value = id
        }
    }

    fun addAtomAt(element: Element, position: Vector3) {
        saveToHistory()
        val newAtom = Atom(nextAtomId++, element, position)
        _molecule.update { it.copy(atoms = it.atoms + newAtom) }
    }

    private fun addBond(atom1Id: Int, atom2Id: Int, type: BondType = BondType.SINGLE) {
        val exists = _molecule.value.bonds.any { 
            (it.atom1Id == atom1Id && it.atom2Id == atom2Id) || (it.atom1Id == atom2Id && it.atom2Id == atom1Id) 
        }
        if (!exists) {
            val newBond = Bond(atom1Id, atom2Id, type)
            _molecule.update { it.copy(bonds = it.bonds + newBond) }
        }
    }

    fun loadDMT() {
        saveToHistory()
        val atoms = mutableListOf<Atom>()
        var id = 1
        
        // --- N,N-Dimetiltriptamina (DMT) - GEOMETRÍA MOLECULAR REAL ---
        atoms.add(Atom(id++, Element.CARBON, Vector3(-1.2f, 1.2f, 0.0f)))  // C4
        atoms.add(Atom(id++, Element.CARBON, Vector3(-2.4f, 0.5f, 0.0f)))  // C5
        atoms.add(Atom(id++, Element.CARBON, Vector3(-2.4f, -0.9f, 0.0f))) // C6
        atoms.add(Atom(id++, Element.CARBON, Vector3(-1.2f, -1.6f, 0.0f))) // C7
        atoms.add(Atom(id++, Element.CARBON, Vector3(0.0f, -0.9f, 0.0f)))  // C7a (Fusión)
        atoms.add(Atom(id++, Element.CARBON, Vector3(0.0f, 0.5f, 0.0f)))   // C3a (Fusión)

        atoms.add(Atom(id++, Element.NITROGEN, Vector3(1.3f, -1.3f, 0.0f))) // N1
        atoms.add(Atom(id++, Element.CARBON, Vector3(2.1f, -0.2f, 0.0f)))   // C2
        atoms.add(Atom(id++, Element.CARBON, Vector3(1.3f, 0.9f, 0.0f)))    // C3

        atoms.add(Atom(id++, Element.CARBON, Vector3(1.8f, 2.3f, 0.3f)))    // C8 (alfa)
        atoms.add(Atom(id++, Element.CARBON, Vector3(3.2f, 2.5f, -0.2f)))   // C9 (beta)
        atoms.add(Atom(id++, Element.NITROGEN, Vector3(3.8f, 3.8f, 0.1f)))  // N (amino terminal)

        atoms.add(Atom(id++, Element.CARBON, Vector3(5.2f, 3.8f, -0.2f)))   // Metilo 1
        atoms.add(Atom(id++, Element.CARBON, Vector3(3.1f, 4.9f, -0.5f)))   // Metilo 2

        atoms.add(Atom(id++, Element.HYDROGEN, Vector3(-1.2f, 2.3f, 0.0f)))
        atoms.add(Atom(id++, Element.HYDROGEN, Vector3(-3.4f, 1.0f, 0.0f)))
        atoms.add(Atom(id++, Element.HYDROGEN, Vector3(-3.4f, -1.4f, 0.0f)))
        atoms.add(Atom(id++, Element.HYDROGEN, Vector3(-1.2f, -2.7f, 0.0f)))
        atoms.add(Atom(id++, Element.HYDROGEN, Vector3(1.6f, -2.3f, 0.0f)))
        atoms.add(Atom(id++, Element.HYDROGEN, Vector3(3.2f, -0.2f, 0.0f)))
        atoms.add(Atom(id++, Element.HYDROGEN, Vector3(1.2f, 2.9f, 1.0f)))
        atoms.add(Atom(id++, Element.HYDROGEN, Vector3(3.8f, 1.8f, 0.4f)))

        val bonds = mutableListOf(
            Bond(1, 2, BondType.AROMATIC), Bond(2, 3, BondType.AROMATIC),
            Bond(3, 4, BondType.AROMATIC), Bond(4, 5, BondType.AROMATIC),
            Bond(5, 6, BondType.AROMATIC), Bond(6, 1, BondType.AROMATIC),
            Bond(5, 7), Bond(7, 8), Bond(8, 9, BondType.DOUBLE), Bond(9, 6),
            Bond(9, 10), Bond(10, 11), Bond(11, 12),
            Bond(12, 13), Bond(12, 14),
            Bond(1, 15), Bond(2, 16), Bond(3, 17), Bond(4, 18), Bond(7, 19), Bond(8, 20),
            Bond(10, 21), Bond(11, 22)
        )

        _molecule.value = Molecule("N,N-Dimetiltriptamina (DMT)", atoms, bonds)
        nextAtomId = id
        generateSurfaceMesh()
    }

    fun clearMolecule() {
        saveToHistory()
        _molecule.value = Molecule("Vacío")
        _surfaceMesh.value = emptyList()
        nextAtomId = 1
    }

    fun generateSurfaceMesh() {
        val atoms = _molecule.value.atoms
        if (atoms.isEmpty()) return
        _isCalculating.value = true
        val edges = mutableListOf<SurfaceEdge>()
        val isoValue = 0.35f // NUBE MÁS GRANDE (Antes 0.5)
        val step = 0.35f   // MALLA MÁS DENSA (Antes 0.4)
        val padding = 3.5f   // MÁS ESPACIO PARA LA NUBE

        val minX = atoms.minOf { it.position.x } - padding
        val maxX = atoms.maxOf { it.position.x } + padding
        val minY = atoms.minOf { it.position.y } - padding
        val maxY = atoms.maxOf { it.position.y } + padding
        val minZ = atoms.minOf { it.position.z } - padding
        val maxZ = atoms.maxOf { it.position.z } + padding

        var x = minX
        while (x <= maxX) {
            var y = minY
            while (y <= maxY) {
                var z = minZ
                while (z <= maxZ) {
                    val p = Vector3(x, y, z)
                    val v0 = calculatePotential(p, atoms)
                    val nx = Vector3(x + step, y, z)
                    val ny = Vector3(x, y + step, z)
                    val nz = Vector3(x, y, z + step)
                    val targets = listOf(nx, ny, nz)
                    for (target in targets) {
                        if (target.x <= maxX && target.y <= maxY && target.z <= maxZ) {
                            val v1 = calculatePotential(target, atoms)
                            if ((v0 > isoValue && v1 <= isoValue) || (v0 <= isoValue && v1 > isoValue)) {
                                val mep = calculateMEP(p, atoms)
                                edges.add(SurfaceEdge(p, target, mep))
                            }
                        }
                    }
                    z += step
                }
                y += step
            }
            x += step
        }
        _surfaceMesh.value = edges
        _isCalculating.value = false
    }

    private fun calculatePotential(p: Vector3, atoms: List<Atom>): Float {
        var potential = 0f
        for (atom in atoms) {
            val dx = p.x - atom.position.x
            val dy = p.y - atom.position.y
            val dz = p.z - atom.position.z
            val d2 = dx*dx + dy*dy + dz*dz
            val r = atom.element.radius
            potential += (r * r) / (d2 + 0.1f)
        }
        return potential
    }

    private fun calculateMEP(p: Vector3, atoms: List<Atom>): Float {
        var esp = 0f
        for (atom in atoms) {
            val dx = p.x - atom.position.x
            val dy = p.y - atom.position.y
            val dz = p.z - atom.position.z
            val dist = sqrt(dx*dx + dy*dy + dz*dz)
            if (dist > 0.1f) {
                val charge = (atom.element.electronegativity - 2.55f)
                esp += charge / dist
            }
        }
        return esp
    }
}
