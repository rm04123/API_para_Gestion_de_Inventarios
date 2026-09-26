package sv.ues.inventarioapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.ues.inventarioapi.exception.ResourceNotFoundException;
import sv.ues.inventarioapi.exception.StockInsuficienteException;
import sv.ues.inventarioapi.model.MovimientoStock;
import sv.ues.inventarioapi.model.Producto;
import sv.ues.inventarioapi.model.TipoMovimiento;
import sv.ues.inventarioapi.repository.MovimientoStockRepository;
import sv.ues.inventarioapi.repository.ProductoRepository;
import sv.ues.inventarioapi.service.impl.MovimientoStockServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para MovimientoStockServiceImpl.
 *
 * Se utiliza Mockito para simular los repositorios y probar
 * únicamente la lógica de negocio del servicio.
 */
@ExtendWith(MockitoExtension.class)
class MovimientoStockServiceImplTest {

    @Mock
    private MovimientoStockRepository movimientoStockRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private MovimientoStockServiceImpl movimientoStockService;

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setId(1L);
        producto.setCodigo("PROD-001");
        producto.setNombre("Teclado");
        producto.setDescripcion("Teclado mecánico");
        producto.setPrecio(new BigDecimal("25.50"));
        producto.setCantidadStock(10);
    }

    /**
     * Verifica que una ENTRADA exitosa aumente el stock del producto
     * y guarde el movimiento.
     */
    @Test
    void registrarEntradaDebeAumentarStockYGuardarMovimiento() {

        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setTipoMovimiento(TipoMovimiento.ENTRADA);
        movimiento.setCantidad(5);
        movimiento.setFechaMovimiento(LocalDateTime.now());
        movimiento.setProducto(producto);

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        when(movimientoStockRepository.save(any(MovimientoStock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoStock resultado = movimientoStockService.registrar(movimiento);

        assertNotNull(resultado);
        assertEquals(TipoMovimiento.ENTRADA, resultado.getTipoMovimiento());
        assertEquals(5, resultado.getCantidad());
        assertEquals(producto, resultado.getProducto());

        // Verificar que el stock se incrementó: 10 + 5 = 15
        assertEquals(15, producto.getCantidadStock());

        verify(productoRepository).findById(1L);
        verify(productoRepository).save(producto);
        verify(movimientoStockRepository).save(any(MovimientoStock.class));
    }

    /**
     * Verifica que una SALIDA exitosa disminuya el stock del producto
     * y guarde el movimiento.
     */
    @Test
    void registrarSalidaDebeDisminuirStockYGuardarMovimiento() {

        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setTipoMovimiento(TipoMovimiento.SALIDA);
        movimiento.setCantidad(3);
        movimiento.setFechaMovimiento(LocalDateTime.now());
        movimiento.setProducto(producto);

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        when(movimientoStockRepository.save(any(MovimientoStock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoStock resultado = movimientoStockService.registrar(movimiento);

        assertNotNull(resultado);
        assertEquals(TipoMovimiento.SALIDA, resultado.getTipoMovimiento());
        assertEquals(3, resultado.getCantidad());

        // Verificar que el stock se decrementó: 10 - 3 = 7
        assertEquals(7, producto.getCantidadStock());

        verify(productoRepository).findById(1L);
        verify(productoRepository).save(producto);
        verify(movimientoStockRepository).save(any(MovimientoStock.class));
    }

    /**
     * Verifica que una SALIDA con stock insuficiente lance
     * StockInsuficienteException y no guarde el movimiento.
     */
    @Test
    void registrarSalidaConStockInsuficienteDebeLanzarExcepcion() {

        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setTipoMovimiento(TipoMovimiento.SALIDA);
        movimiento.setCantidad(15); // Mayor al stock actual (10)
        movimiento.setFechaMovimiento(LocalDateTime.now());
        movimiento.setProducto(producto);

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        assertThrows(
                StockInsuficienteException.class,
                () -> movimientoStockService.registrar(movimiento)
        );

        // Verificar que el stock NO cambió
        assertEquals(10, producto.getCantidadStock());

        verify(productoRepository).findById(1L);
        verify(productoRepository, never()).save(any());
        verify(movimientoStockRepository, never()).save(any());
    }

    /**
     * Verifica que registrar un movimiento para un producto inexistente
     * lance ResourceNotFoundException.
     */
    @Test
    void registrarMovimientoProductoInexistenteDebeLanzarExcepcion() {

        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setTipoMovimiento(TipoMovimiento.ENTRADA);
        movimiento.setCantidad(5);
        movimiento.setFechaMovimiento(LocalDateTime.now());
        movimiento.setProducto(new Producto()); // producto con id null
        movimiento.getProducto().setId(99L);

        when(productoRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> movimientoStockService.registrar(movimiento)
        );

        verify(productoRepository).findById(99L);
        verify(productoRepository, never()).save(any());
        verify(movimientoStockRepository, never()).save(any());
    }

    /**
     * Verifica que obtenerHistorial retorne los movimientos
     * proporcionados por el repositorio.
     */
    @Test
    void obtenerHistorialDebeRetornarListaDeMovimientos() {

        MovimientoStock movimiento1 = new MovimientoStock();
        movimiento1.setId(1L);
        movimiento1.setTipoMovimiento(TipoMovimiento.ENTRADA);
        movimiento1.setCantidad(5);
        movimiento1.setFechaMovimiento(LocalDateTime.now());
        movimiento1.setProducto(producto);

        MovimientoStock movimiento2 = new MovimientoStock();
        movimiento2.setId(2L);
        movimiento2.setTipoMovimiento(TipoMovimiento.SALIDA);
        movimiento2.setCantidad(3);
        movimiento2.setFechaMovimiento(LocalDateTime.now());
        movimiento2.setProducto(producto);

        when(movimientoStockRepository.findAll())
                .thenReturn(List.of(movimiento1, movimiento2));

        List<MovimientoStock> resultado = movimientoStockService.obtenerHistorial();

        assertEquals(2, resultado.size());
        assertEquals(TipoMovimiento.ENTRADA, resultado.get(0).getTipoMovimiento());
        assertEquals(TipoMovimiento.SALIDA, resultado.get(1).getTipoMovimiento());

        verify(movimientoStockRepository).findAll();
    }
}