package io.smallrye.stork;

import io.smallrye.stork.integration.DefaultStorkInfrastructure;

public class StorkTestUtils {
    @SuppressWarnings("deprecation")
    public static Stork getNewStorkInstance() {
        return new Stork(new DefaultStorkInfrastructure());
    }
}
