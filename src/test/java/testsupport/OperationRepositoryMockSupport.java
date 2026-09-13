package testsupport;

import org.joupen.domain.OperationMetadata;
import org.joupen.domain.OperationResult;
import org.joupen.domain.PlayerEntity;
import org.joupen.domain.PlayerOperation;
import org.joupen.repository.PlayerRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

public final class OperationRepositoryMockSupport {
    private OperationRepositoryMockSupport() {
    }

    public static void enable(PlayerRepository repo) {
        List<PlayerOperation> operations = new ArrayList<>();
        lenient().when(repo.applyOperation(anyString(), any(), anyString(), any())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            OperationMetadata metadata = invocation.getArgument(1);
            String request = invocation.getArgument(2);
            BiFunction<Optional<PlayerEntity>, List<PlayerOperation>, PlayerOperation> action = invocation.getArgument(3);
            Optional<PlayerEntity> current = repo.findByName(name);
            PlayerOperation operation = action.apply(current, operations);
            operation.setId((long) operations.size() + 1);
            operation.setMetadata(metadata);
            operation.setRequest(request);
            if (current.isPresent()) repo.updateByName(operation.getAfter(), name);
            else repo.save(operation.getAfter());
            operations.add(operation);
            return new OperationResult(operation, true);
        });
    }
}
