package io.github.mightycode.mightylib.test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// The key of the series of reference images, from real GL_VENDOR / GL_RENDERER strings
class RendererKeyTest {
    private static String hardware(String os, String vendor, String renderer) {
        return RendererKey.of(os, vendor, renderer);
    }

    // One series per software driver, whatever the processor (a run of the CI on any runner gives it)
    @Test
    void softwareRenderersHaveOneKeyWhateverTheProcessor() {
        assertEquals("linux-mesa-llvmpipe", RendererKey.of("Linux", "Mesa", "llvmpipe (LLVM 15.0.7, 256 bits)"));
        assertEquals("linux-mesa-llvmpipe", RendererKey.of("Linux", "Mesa", "llvmpipe (LLVM 20.1.2, 128 bits)"));
        assertEquals("linux-mesa-llvmpipe", RendererKey.of("Linux", "Mesa/X.org", "llvmpipe (LLVM 12.0.0, 256 bits)"));
        assertEquals("linux-mesa-llvmpipe", RendererKey.of("Linux", "Mesa", "lavapipe"));
        assertEquals("linux-mesa-softpipe", RendererKey.of("Linux", "Mesa", "softpipe"));
        assertEquals("windows-mesa-llvmpipe", RendererKey.of("Windows 11", "Mesa", "llvmpipe (LLVM 17.0.6, 256 bits)"));
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
        assertEquals("unknown-unknown", RendererKey.of(null, null, null));
    }

    @Test
    void thePropertyReplacesTheKey() {
        assertEquals("linux-mesa-llvmpipe", RendererKey.resolve(" linux-mesa-llvmpipe ", "Windows 11",
                "NVIDIA Corporation", "NVIDIA GeForce RTX 3070/PCIe/SSE2"));
        assertEquals("windows-nvidia", RendererKey.resolve("", "Windows 11", "NVIDIA Corporation", "GeForce"));
        assertEquals("windows-nvidia", RendererKey.resolve(null, "Windows 11", "NVIDIA Corporation", "GeForce"));
        assertThrows(IllegalStateException.class, () -> RendererKey.resolve("Linux Mesa", "Linux", "", ""));
        assertThrows(IllegalStateException.class, () -> RendererKey.resolve("../linux", "Linux", "", ""));
    }
}
