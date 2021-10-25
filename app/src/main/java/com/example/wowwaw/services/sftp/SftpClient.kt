package com.example.wowwaw.services.sftp

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SSHException
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import timber.log.Timber
import java.io.IOException

class SftpClient(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String
) {

    fun withSession(f: (SFTPClient) -> Unit) {
        val sshClient = SSHClient()
        sshClient.addHostKeyVerifier(PromiscuousVerifier())
        try {
            sshClient.connect(host, port)
        } catch (ex: IOException) {
            Timber.e(ex, "Cant connect to SFTP server: $ex")
            return
        }
        try {
            sshClient.authPassword(username, password)
        } catch (ex: SSHException) {
            Timber.e(ex, "Cant auth on SFTP server: $ex")
            return
        }

        sshClient.newSFTPClient().use {
            try {
                f(it)
            } catch (ex: Exception) {
                Timber.e(ex, "Error in sftp context: $ex")
            }
        }
        try {
            sshClient.close()
        } catch (ex: IOException) {
            Timber.e(ex, "Error on sftp close: $ex")
        }
    }
}
