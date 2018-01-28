package com.github.shyiko.ktlint.internal

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transfer.TransferEvent
import org.eclipse.aether.transfer.TransferListener
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator
import java.io.File

class MavenDependencyResolver(
    baseDir: File,
    val repositories: Iterable<RemoteRepository>,
    forceUpdate: Boolean
) {

    private val repoSystem: RepositorySystem
    private val session: RepositorySystemSession

    init {
        val locator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
        locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
        locator.setErrorHandler(object : DefaultServiceLocator.ErrorHandler() {
            override fun serviceCreationFailed(type: Class<*>?, impl: Class<*>?, ex: Throwable) {
                throw ex
            }
        })
        repoSystem = locator.getService(RepositorySystem::class.java)
        session = MavenRepositorySystemUtils.newSession()
        session.localRepositoryManager = repoSystem.newLocalRepositoryManager(session, LocalRepository(baseDir))
        session.updatePolicy = if (forceUpdate) {
            RepositoryPolicy.UPDATE_POLICY_ALWAYS
        } else {
            RepositoryPolicy.UPDATE_POLICY_NEVER
        }
    }

    fun setTransferEventListener(listener: (event: TransferEvent) -> Unit) {
        (session as DefaultRepositorySystemSession).transferListener = object : TransferListener {

            override fun transferProgressed(event: TransferEvent) = listener(event)
            override fun transferStarted(event: TransferEvent) = listener(event)
            override fun transferInitiated(event: TransferEvent) = listener(event)
            override fun transferSucceeded(event: TransferEvent) = listener(event)
            override fun transferCorrupted(event: TransferEvent) = listener(event)
            override fun transferFailed(event: TransferEvent) = listener(event)
        }
    }

    fun resolve(vararg artifacts: Artifact): Collection<File> {
        val collectRequest = CollectRequest()
        artifacts.forEach {
            collectRequest.addDependency(Dependency(it, "compile"))
        }
        repositories.forEach {
            collectRequest.addRepository(it)
        }
        val node = repoSystem.collectDependencies(session, collectRequest).root
        repoSystem.resolveDependencies(session, DependencyRequest().apply { root = node })
        return PreorderNodeListGenerator().apply { node.accept(this) }.files
    }
}
