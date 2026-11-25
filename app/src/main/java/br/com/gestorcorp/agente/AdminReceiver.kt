package br.com.gestorcorp.agente

import android.app.admin.DeviceAdminReceiver

// Administrador do aparelho: identifica o agente como gerenciado e permite
// impedir a desinstalação.
class AdminReceiver : DeviceAdminReceiver()