/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.transport.net

import platform.posix.*
import kotlin.native.concurrent.*

actual val EAGAIN_ERROR: Int = EAGAIN

@SharedImmutable
actual val errorsMapping = setOf(
    TcpError(name = "EPERM", id = 1, description = "(Operation not permitted) Operation not permitted"),
    TcpError(name = "ENOENT", id = 2, description = "(No such file or directory) No such file or directory"),
    TcpError(name = "ESRCH", id = 3, description = "(Nosuch process) No such process"),
    TcpError(name = "EINTR", id = 4, description = "(Interrupted system call) Interrupted system call"),
    TcpError(name = "EIO", id = 5, description = "(Input/output error) Input/output error"),
    TcpError(name = "ENXIO", id = 6, description = "(Device not configured) No such device or address"),
    TcpError(name = "E2BIG", id = 7, description = "(Argument list too long) Argument list too long"),
    TcpError(name = "ENOEXEC", id = 8, description = "(Exec format error) Exec format error"),
    TcpError(name = "EBADF", id = 9, description = "(Bad file descriptor) Bad file descriptor"),
    TcpError(name = "ECHILD", id = 10, description = "(No child processes) No child processes"),
    TcpError(name = "EDEADLK", id = 11, description = "(Resource deadlock avoided) Resource temporarily unavailable"),
    TcpError(name = "ENOMEM", id = 12, description = "(Cannot allocate memory) Cannot allocate memory"),
    TcpError(name = "EACCES", id = 13, description = "(Permission denied) Permission denied"),
    TcpError(name = "EFAULT", id = 14, description = "(Bad address) Bad address"),
    TcpError(name = "ENOTBLK", id = 15, description = "(Block device required) Block device required"),
    TcpError(name = "EBUSY", id = 16, description = "(Device busy) Device or resource busy"),
    TcpError(name = "EEXIST", id = 17, description = "(File exists) File exists"),
    TcpError(name = "EXDEV", id = 18, description = "(Cross-device link) Invalid cross-device link"),
    TcpError(name = "ENODEV", id = 19, description = "(Operation not supported by device) No such device"),
    TcpError(name = "ENOTDIR", id = 20, description = "(Not a directory) Not a directory"),
    TcpError(name = "EISDIR", id = 21, description = "(Is a directory) Is a directory"),
    TcpError(name = "EINVAL", id = 22, description = "(Invalid argument) Invalid argument"),
    TcpError(name = "ENFILE", id = 23, description = "(Too many open files in system) Too many open files in system"),
    TcpError(name = "EMFILE", id = 24, description = "(Too many open files) Too many open files"),
    TcpError(name = "ENOTTY", id = 25, description = "(Inappropriate ioctl for device) Inappropriate ioctl for device"),
    TcpError(name = "ETXTBSY", id = 26, description = "(Text file busy) Text file busy"),
    TcpError(name = "EFBIG", id = 27, description = "(File too large) File too large"),
    TcpError(name = "ENOSPC", id = 28, description = "(No space left ondevice) No space left on device"),
    TcpError(name = "ESPIPE", id = 29, description = "(Illegal seek) Illegal seek"),
    TcpError(name = "EROFS", id = 30, description = "(Read-only filesystem) Read-only file system"),
    TcpError(name = "EMLINK", id = 31, description = "(Too many links) Too many links"),
    TcpError(name = "EPIPE", id = 32, description = "(Broken pipe) Broken pipe"),
    TcpError(name = "EDOM", id = 33, description = "(Numerical argument out of domain) Numerical argument out of domain"),
    TcpError(name = "ERANGE", id = 34, description = "(Result too large) Numerical result out of range"),
    TcpError(name = "EAGAIN", id = 35, description = "(Resource temporarily unavailable) Resource deadlock avoided"),
    TcpError(name = "EWOULDBLOCK", id = 35, description = "(Resource temporarily unavailable) File name too long"),
    TcpError(name = "EINPROGRESS", id = 36, description = "(Operation now in progress) No locks available"),
    TcpError(name = "EALREADY", id = 37, description = "(Operational ready in progress) Function not implemented"),
    TcpError(name = "ENOTSOCK", id = 38, description = "(Socket operation on non-socket) Directory not empty"),
    TcpError(
        name = "EDESTADDRREQ",
        id = 39,
        description = "(Destination address required) Too many levels of symbolic links"
    ),
    TcpError(name = "EMSGSIZE", id = 40, description = "(Message too long) Unknown error 41"),
    TcpError(name = "EPROTOTYPE", id = 41, description = "(Protocol wrong type for socket) No message of desired type"),
    TcpError(name = "ENOPROTOOPT", id = 42, description = "(Protocol not available) Identifier removed"),
    TcpError(name = "EPROTONOSUPPORT", id = 43, description = "(Protocol not supported) Channel number out of range"),
    TcpError(name = "ESOCKTNOSUPPORT", id = 44, description = "(Socket type not supported) Level 2 not synchronized"),
    TcpError(name = "EOPNOTSUPP", id = 45, description = "(Operation not supported) Level 3 halted"),
    TcpError(name = "ENOTSUP", id = 45, description = "(Operation not supported) Level 3 reset"),
    TcpError(name = "EPFNOSUPPORT", id = 46, description = "(Protocol family not supported) Link number out of range"),
    TcpError(
        name = "EAFNOSUPPORT",
        id = 47,
        description = "(Address family not supported by protocol family) Protocol driver not attached"
    ),
    TcpError(name = "EADDRINUSE", id = 48, description = "(Address already inuse) No CSI structure available"),
    TcpError(name = "EADDRNOTAVAIL", id = 49, description = "(Can't assign requested address) Level 2 halted"),
    TcpError(name = "ENETDOWN", id = 50, description = "(Network is down) Invalid exchange"),
    TcpError(name = "ENETUNREACH", id = 51, description = "(Network is unreachable) Invalid request descriptor"),
    TcpError(name = "ENETRESET", id = 52, description = "(Network dropped connection on reset) Exchange full"),
    TcpError(name = "ECONNABORTED", id = 53, description = "(Software caused connection abort) No anode"),
    TcpError(name = "ECONNRESET", id = 54, description = "(Connection reset b peer) Invalid request code"),
    TcpError(name = "ENOBUFS", id = 55, description = "(No buffer space available) Invalid slot"),
    TcpError(name = "EISCONN", id = 56, description = "(Socket is already connected) Unknown error 58"),
    TcpError(name = "ENOTCONN", id = 57, description = "(Socket is not connected) Bad font file format"),
    TcpError(name = "ESHUTDOWN", id = 58, description = "(Can't send after socket shutdown) Device not a stream"),
    TcpError(name = "ETOOMANYREFS", id = 59, description = "(Too many references:can't splice) No data available"),
    TcpError(name = "ETIMEDOUT", id = 60, description = "(Operation timed out) Timer expired"),
    TcpError(name = "ECONNREFUSED", id = 61, description = "(Connection refused) Out of streams resources"),
    TcpError(name = "ELOOP", id = 62, description = "(Too many levels of symbolic links) Machine is not on the network"),
    TcpError(name = "ENAMETOOLONG", id = 63, description = "(File name too long) Package not installed"),
    TcpError(name = "EHOSTDOWN", id = 64, description = "(Host is down) Object is remote"),
    TcpError(name = "EHOSTUNREACH", id = 65, description = "(No route to host) Link has been severed"),
    TcpError(name = "ENOTEMPTY", id = 66, description = "(Directory not empty) Advertise error"),
    TcpError(name = "EPROCLIM", id = 67, description = "(Too many processes) Srmount error"),
    TcpError(name = "EUSERS", id = 68, description = "(Too many users) Communication error on send"),
    TcpError(name = "EDQUOT", id = 69, description = "(Discquota exceeded) Protocol error"),
    TcpError(name = "ESTALE", id = 70, description = "(StaleNFS file handle) Multihop attempted"),
    TcpError(name = "EREMOTE", id = 71, description = "(Too many levels of remote in path) RFS specific error"),
    TcpError(name = "EBADRPC", id = 72, description = "(RPC struct is bad) Bad message"),
    TcpError(name = "ERPCMISMATCH", id = 73, description = "(RPC version wrong) Value too large for defined data type"),
    TcpError(name = "EPROGUNAVAIL", id = 74, description = "(RPC prog.not avail) Name not unique on network"),
    TcpError(name = "EPROGMISMATCH", id = 75, description = "(Program version wrong) File descriptor in bad state"),
    TcpError(name = "EPROCUNAVAIL", id = 76, description = "(Bad procedure for program) Remote address changed"),
    TcpError(name = "ENOLCK", id = 77, description = "(No locks available) Can not access a needed shared library"),
    TcpError(name = "ENOSYS", id = 78, description = "(Function not implemented) Accessing a corrupted shared library"),
    TcpError(name = "EFTYPE", id = 79, description = "(Inappropriate file type or format) .lib section in a.out corrupted"),
    TcpError(
        name = "EAUTH",
        id = 80,
        description = "(Authentication error) Attempting to link in too many shared libraries"
    ),
    TcpError(name = "ENEEDAUTH", id = 81, description = "(Need authenticator) Cannot exec a shared library directly"),
    TcpError(
        name = "EIDRM",
        id = 82,
        description = "(Identifier removed) Invalid or incomplete multibyte or wide character"
    ),
    TcpError(
        name = "ENOMSG",
        id = 83,
        description = "(No message of desired type) Interrupted system call should be restarted"
    ),
    TcpError(name = "EOVERFLOW", id = 84, description = "(Value too large to be stored in datatype) Streams pipe error"),
    TcpError(name = "ECANCELED", id = 85, description = "(Operation canceled) Too many users"),
    TcpError(name = "EILSEQ", id = 86, description = "(Illegal byte sequence) Socket operation on non-socket"),
    TcpError(name = "ENOATTR", id = 87, description = "(Attribute not found) Destination address required"),
    TcpError(name = "EDOOFUS", id = 88, description = "(Programming error) Message too long"),
    TcpError(name = "EBADMSG", id = 89, description = "(Bad message) Protocol wrong type for socket"),
    TcpError(name = "EMULTIHOP", id = 90, description = "(Multihop attempted) Protocol not available"),
    TcpError(name = "ENOLINK", id = 91, description = "(Link has been severed) Protocol not supported"),
    TcpError(name = "EPROTO", id = 92, description = "(Protocol error) Socket type not supported"),
    TcpError(name = "ENOTCAPABLE", id = 93, description = "(Capabilities insufficient) Operation not supported"),
    TcpError(name = "ECAPMODE", id = 94, description = "(Not permitted incapability mode) Protocol family not supported")
).associateBy { it.id }