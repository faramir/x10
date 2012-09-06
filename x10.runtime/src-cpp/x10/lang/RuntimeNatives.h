/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright Australian National University 2012.
 */

#ifndef X10_LANG_RUNTIMENATIVES_H
#define X10_LANG_RUNTIMENATIVES_H

#include <x10aux/config.h>

namespace x10 {
    namespace io {
        class Reader;
        class Writer;
    }
}

namespace x10 {
    namespace lang {
        class RuntimeNatives {
        public:
            static x10::io::Reader* execForRead(const char *command);
            static x10::io::Writer* execForWrite(const char *command);
        };
    }
}

#endif /* X10_LANG_RUNTIMENATIVES_H */

// vim:tabstop=4:shiftwidth=4:expandtab