//package service;
//
//import org.joupen.repository.PlayerRepository;
//import org.joupen.service.MigrationService;
//import org.joupen.utils.JoupenProperties;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//
//import java.util.ArrayList;
//
//import static org.mockito.Mockito.*;
//
//public class MigrationServiceTest {
//
//    @Mock
//    private PlayerRepository repo;
//    @InjectMocks
//    private MigrationService service;
//
//    @BeforeEach
//    void setUp() {
//        repo = mock(PlayerRepository.class);
//        service = new MigrationService(repo);
//    }
//
//    @Test
//    void migrate_whenUseSqlIsFalse_shouldNotMigrate() {
//        JoupenProperties.useSql = false;
//        service.migrate();
//        verify(repo, never()).findAll();
//    }
//
//    @Test
//    void migrate_whenUseSqlIsTrue_shouldMigrateFromFile() {
//        JoupenProperties.useSql = true;
//        when(repo.findAll()).thenReturn(new ArrayList<>());
//        service.migrate();
//        verify(repo, atLeastOnce()).findAll();
//    }
//}
