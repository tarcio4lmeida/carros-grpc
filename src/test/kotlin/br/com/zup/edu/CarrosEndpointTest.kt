package br.com.zup.edu

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows

import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CarrosEndpointTest(
    val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub,
    val repository: CarroRepository
){
    @BeforeEach
    internal fun setUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve adicionar um novo carro`() {
        // Acao
        val response = grpcClient.adicionar(
            CarroRequest.newBuilder()
                .setModelo("Gol")
                .setPlaca("HIO-9987")
                .build()
        )

        // validacao
        with(response) {
            assertNotNull(id)
            assertTrue(repository.existsById(id))
        }
    }
    @Test
    internal fun `nao deve novo carro quando carro com placa já existir`() {
       //cenario
        val existente = repository.save(
            Carro(
                modelo = "Gol",
                placa = "OLJ-9876"
            )
        )
        //acao
        val error = assertThrows(StatusRuntimeException::class.java) {
            grpcClient.adicionar(
                CarroRequest.newBuilder()
                    .setModelo("Gol")
                    .setPlaca(existente.placa)
                    .build()
            )
        }
        //validacao
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("carro com placa existente", status.description)
        }
    }

    @Test
    fun `nao deve adicionar novo carro quando dados de entrada forem invalidos`() {
        //acao
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(
                CarroRequest.newBuilder()
                    .setModelo("")
                    .setPlaca("")
                    .build()
            )
        }
        //validacao
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos", status.description)
        }
    }

    @Factory
    class Carros {

        @Singleton
        fun blockingStub(
            @GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel
        ): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub {
            return CarrosGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

}