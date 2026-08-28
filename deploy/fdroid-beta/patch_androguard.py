#!/usr/bin/env python3
from pathlib import Path


path = Path("/usr/lib/python3/dist-packages/androguard/core/bytecodes/axml/__init__.py")
source = path.read_text(encoding="utf-8")
old = '''                # Next, we should have the resource key symbol table
                self.buff.set_idx(current_package.header.start + current_package.keyStrings)
                key_sp_header = ARSCHeader(self.buff, expected_type=RES_STRING_POOL_TYPE)
                mKeyStrings = StringBlock(self.buff, key_sp_header)

                # Add them to the dict of read packages
                self.packages[package_name].append(current_package)
                self.packages[package_name].append(mTableStrings)
                self.packages[package_name].append(mKeyStrings)

                pc = PackageContext(current_package, self.stringpool_main, mTableStrings, mKeyStrings)
                log.debug("Constructed a PackageContext: %s", pc)

                # skip to the first header in this table package chunk
                next_idx = res_header.start + res_header.header_size + type_sp_header.size + key_sp_header.size
'''
new = '''                if current_package.keyStrings:
                    # Next, we should have the resource key symbol table
                    self.buff.set_idx(current_package.header.start + current_package.keyStrings)
                    key_sp_header = ARSCHeader(self.buff, expected_type=RES_STRING_POOL_TYPE)
                    mKeyStrings = StringBlock(self.buff, key_sp_header)
                    key_sp_size = key_sp_header.size
                else:
                    key_sp_header = None
                    mKeyStrings = None
                    key_sp_size = 0

                self.packages[package_name].append(current_package)
                self.packages[package_name].append(mTableStrings)
                if mKeyStrings is not None:
                    self.packages[package_name].append(mKeyStrings)

                pc = PackageContext(current_package, self.stringpool_main, mTableStrings, mKeyStrings)
                log.debug("Constructed a PackageContext: %s", pc)

                next_idx = res_header.start + res_header.header_size + type_sp_header.size + key_sp_size
'''

if new in source:
    raise SystemExit("Androguard is already patched")
if old not in source:
    raise SystemExit("Expected Androguard resource parser block was not found")

path.write_text(source.replace(old, new, 1), encoding="utf-8")
