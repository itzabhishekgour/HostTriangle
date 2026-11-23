TERMUX_PKG_HOMEPAGE=https://talloc.samba.org/talloc/doc/html/index.html
TERMUX_PKG_DESCRIPTION="Hierarchical, reference counted memory pool system with destructors"
TERMUX_PKG_LICENSE="LGPL-3.0"
TERMUX_PKG_MAINTAINER="@termux"
TERMUX_PKG_VERSION=2.5.0
TERMUX_PKG_SRCURL=https://www.samba.org/ftp/talloc/talloc-${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_EXTRA_CONFIGURE_ARGS="--cross-compile --cross-execute= --disable-python"

termux_step_pre_configure() {
	export PYTHONHASHSEED=1
}
