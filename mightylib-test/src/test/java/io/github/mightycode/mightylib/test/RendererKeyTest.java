package io.github.mightycode.mightylib.test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The key of the series of reference images, from real GL_VENDOR / GL_RENDERER strings and processors
class RendererKeyTest {
    private static final String INTEL = "GenuineIntel";
    private static final String AMD = "AuthenticAMD";

    // A driver of a graphic card: the processor is not part of the key
    private static String hardware(String os, String vendor, String renderer) {
        String key = RendererKey.of(os, vendor, renderer, INTEL);
        assertEquals(key, RendererKey.of(os, vendor, renderer, AMD), "the processor changed the key");
        return key;
    }

    @Test
    void softwareRenderersNameTheMakerOfTheProcessor() {
        assertEquals("linux-mesa-llvmpipe-intel", RendererKey.of("Linux", "Mesa", "llvmpipe (LLVM 15.0.7, 256 bits)",
                INTEL));
        assertEquals("linux-mesa-llvmpipe-amd", RendererKey.of("Linux", "Mesa", "llvmpipe (LLVM 20.1.2, 128 bits)",
                AMD));
        assertEquals("linux-mesa-llvmpipe-amd", RendererKey.of("Linux", "Mesa/X.org", "llvmpipe (LLVM 12.0.0, 256 bits)",
                AMD));
        assertEquals("linux-mesa-softpipe-intel", RendererKey.of("Linux", "Mesa", "softpipe", INTEL));
        // PROCESSOR_IDENTIFIER of Windows: "AMD64" is the architecture of Intel processors too
        assertEquals("windows-mesa-llvmpipe-intel", RendererKey.of("Windows 11", "Mesa", "llvmpipe (LLVM 17.0.6, 256 bits)",
                "Intel64 Family 6 Model 158 Stepping 10, GenuineIntel"));
        assertEquals("windows-mesa-llvmpipe-amd", RendererKey.of("Windows 11", "Mesa", "llvmpipe (LLVM 17.0.6, 256 bits)",
                "AMD64 Family 25 Model 33 Stepping 0, AuthenticAMD"));
        // Without vendor id: the architecture
        assertEquals("linux-mesa-llvmpipe-arm", RendererKey.of("Linux", "Mesa", "llvmpipe (LLVM 15.0.7, 128 bits)",
                "aarch64"));
        assertEquals("linux-mesa-llvmpipe-unknown", RendererKey.of("Linux", "Mesa", "llvmpipe", null));
    }

    @Test
    void mesaHardwareDrivers() {
        assertEquals("linux-mesa-radeonsi", hardware("Linux", "AMD",
                "AMD Radeon RX 580 Series (radeonsi, polaris10, LLVM 15.0.7, DRM 3.49, 6.1.0-13-amd64)"));
        assertEquals("linux-mesa-amd", hardware("Linux", "X.Org", "AMD RAVEN (DRM 3.42.0, 5.15.0, LLVM 12.0.0)"));
        // The renderer of Mesa for Intel (iris, crocus) does not name its driver
        assertEquals("linux-mesa-intel", hardware("Linux", "Intel", "Mesa Intel(R) UHD Graphics 620 (KBL GT2)"));
        assertEquals("linux-mesa-intel", hardware("Linux", "Intel", "Mesa Intel(R) Xe Graphics (TGL GT2)"));
        assertEquals("linux-mesa-nouveau", hardware("Linux", "nouveau", "NV137"));
        assertEquals("linux-mesa-d3d12", hardware("Linux", "Microsoft Corporation", "D3D12 (NVIDIA GeForce RTX 3070)"));
        assertEquals("linux-mesa-zink", hardware("Linux", "Collabora Ltd", "zink Vulkan 1.3(NVIDIA GeForce"
                + " RTX 3070 (NVIDIA_PROPRIETARY))"));
    }

    @Test
    void proprietaryDrivers() {
        assertEquals("windows-nvidia", hardware("Windows 10", "NVIDIA Corporation", "NVIDIA GeForce RTX 3070/PCIe/SSE2"));
        assertEquals("linux-nvidia", hardware("Linux", "NVIDIA Corporation", "NVIDIA GeForce GTX 1060 6GB/PCIe/SSE2"));
        assertEquals("windows-amd", hardware("Windows 11", "ATI Technologies Inc.", "AMD Radeon RX 6700 XT"));
        assertEquals("windows-intel", hardware("Windows 11", "Intel", "Intel(R) UHD Graphics 620"));
        // "Iris" is a name of Intel cards, not the driver iris of Mesa
        assertEquals("windows-intel", hardware("Windows 11", "Intel", "Intel(R) Iris(R) Xe Graphics"));
        assertEquals("macos-apple", hardware("Mac OS X", "Apple", "Apple M1"));
        assertEquals("macos-intel", hardware("Mac OS X", "Intel Inc.", "Intel(R) Iris(TM) Plus Graphics 655"));
    }

    @Test
    void anUnknownDriverIsItsVendorReduced() {
        assertEquals("windows-microsoft-corporation", hardware("Windows 11", "Microsoft Corporation", "GDI Generic"));
        assertEquals("freebsd-unknown", hardware("FreeBSD", "", ""));
        assertEquals("unknown-unknown", RendererKey.of(null, null, null, null));
    }

    @Test
    void thePropertyReplacesTheKey() {
        assertEquals("linux-mesa-llvmpipe-amd", RendererKey.resolve(" linux-mesa-llvmpipe-amd ", "Windows 11",
                "NVIDIA Corporation", "NVIDIA GeForce RTX 3070/PCIe/SSE2", INTEL));
        assertEquals("windows-nvidia", RendererKey.resolve("", "Windows 11", "NVIDIA Corporation", "GeForce", INTEL));
        assertEquals("windows-nvidia", RendererKey.resolve(null, "Windows 11", "NVIDIA Corporation", "GeForce", INTEL));
        assertThrows(IllegalStateException.class, () -> RendererKey.resolve("Linux Mesa", "Linux", "", "", INTEL));
        assertThrows(IllegalStateException.class, () -> RendererKey.resolve("../linux", "Linux", "", "", INTEL));
    }
}
