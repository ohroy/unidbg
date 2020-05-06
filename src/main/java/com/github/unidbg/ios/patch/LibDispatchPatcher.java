package com.github.unidbg.ios.patch;

import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.pointer.UnicornPointer;
import com.github.unidbg.spi.ModulePatcher;
import com.github.unidbg.utils.Inspector;
import com.sun.jna.Pointer;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneEncoded;
import keystone.KeystoneMode;

import java.util.Arrays;

public class LibDispatchPatcher extends ModulePatcher {

    public LibDispatchPatcher() {
        super("/usr/lib/system/libdispatch.dylib");
    }

    protected void patch32(Emulator<?> emulator, Module module) {
        Pointer pointer = UnicornPointer.pointer(emulator, module.base + 0x00211b4); // dispatch_semaphore_wait
        assert pointer != null;
        byte[] code = pointer.getByteArray(0, 4);
        if (!Arrays.equals(code, new byte[]{ 0x4, (byte) 0xc0, (byte) 0x9f, (byte) 0xe5 })) { // arm ldr ip, [pc, #4]
            throw new IllegalStateException(Inspector.inspectString(code, "patch32 pointer=" + pointer));
        }
        try (Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.Arm)) {
            KeystoneEncoded encoded = keystone.assemble("bx lr");
            byte[] patch = encoded.getMachineCode();
            if (patch.length != code.length) {
                throw new IllegalStateException(Inspector.inspectString(patch, "patch32 length=" + patch.length));
            }
            pointer.write(0, patch, 0, patch.length);
        }
    }

    @Override
    protected void patch64(Emulator<?> emulator, Module module) {
    }

}
