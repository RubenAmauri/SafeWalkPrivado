package com.safewalk.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safewalk.app.model.ZonaFrecuente
import com.safewalk.app.repository.AvistamientoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ZonasFrecuentesViewModel : ViewModel() {

    private val _zonas = MutableStateFlow<List<ZonaFrecuente>>(emptyList())
    val zonas: StateFlow<List<ZonaFrecuente>> = _zonas

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _guardadoExitoso = MutableStateFlow(false)
    val guardadoExitoso: StateFlow<Boolean> = _guardadoExitoso

    private val _zonaParaEditar = MutableStateFlow<ZonaFrecuente?>(null)
    val zonaParaEditar: StateFlow<ZonaFrecuente?> = _zonaParaEditar
    private val _mostrarFormulario = MutableStateFlow(false)
    val mostrarFormulario: StateFlow<Boolean> = _mostrarFormulario


    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _cargando.value = true
            _zonas.value = AvistamientoRepository.getZonasFrecuentes()
            _cargando.value = false
        }
    }

    fun seleccionarParaEditar(zona: ZonaFrecuente) {
        _zonaParaEditar.value = zona
    }

    fun limpiarEdicion() {
        _zonaParaEditar.value = null
        _guardadoExitoso.value = false
    }

    fun agregar(nombre: String, latitud: Double, longitud: Double) {
        viewModelScope.launch {
            _cargando.value = true
            val nueva = AvistamientoRepository.agregarZonaFrecuente(nombre, latitud, longitud)
            if (nueva != null) {
                _zonas.value = _zonas.value + nueva
            }
            _guardadoExitoso.value = true
            _cargando.value = false
        }
    }

    fun editar(zonaId: String, nombre: String, latitud: Double, longitud: Double) {
        viewModelScope.launch {
            _cargando.value = true
            AvistamientoRepository.editarZonaFrecuente(zonaId, nombre, latitud, longitud)
            _zonas.value = _zonas.value.map {
                if (it.id == zonaId) it.copy(nombre = nombre, latitud = latitud, longitud = longitud)
                else it
            }
            _guardadoExitoso.value = true
            _cargando.value = false
        }
    }

    fun eliminar(zonaId: String) {
        viewModelScope.launch {
            AvistamientoRepository.eliminarZonaFrecuente(zonaId)
            _zonas.value = _zonas.value.filter { it.id != zonaId }
        }
    }
    fun abrirFormulario() {
        _mostrarFormulario.value = true
    }

    fun cerrarFormulario() {
        _mostrarFormulario.value = false
        limpiarEdicion()
    }
}