// Jquery Serialization Plug-In
// https://tdanemar.wordpress.com/2010/08/24/jquery-serialize-method-and-checkboxes/
$.fn.serialize = function (options) {
    return $.param(this.serializeArray(options));
};

$.fn.serializeArray = function (options) {
    const o = $.extend({
        checkboxesAsBools: false
    }, options || {});

    const rselectTextarea = /select|textarea/i;
    const rinput = /text|hidden|password|search/i;

    return this.map(function () {
        return this.elements ? $.makeArray(this.elements) : this;
    })
        .filter(function () {
            return this.name && !this.disabled &&
                (this.checked
                    || (o.checkboxesAsBools && this.type === 'checkbox')
                    || rselectTextarea.test(this.nodeName)
                    || rinput.test(this.type));
        })
        .map(function (i, elem) {
            const val = $(this).val();
            return val == null ?
                null :
                $.isArray(val) ?
                    $.map(val, function (val, i) {
                        return {name: elem.name, value: val};
                    }) :
                    {
                        name: elem.name,
                        value: (o.checkboxesAsBools && this.type === 'checkbox') ? //moar ternaries!
                            (this.checked ? 'true' : 'false') :
                            val
                    };
        }).get();
};

// CONTEXT MENU

var contextMenu = document.getElementById("context-menu");

var menuTargetClass = "table";

var clientX = 0;
var clientY = 0;

document.addEventListener("click", function (event) {
    var target = event.target;

    const isClickInside = contextMenu.contains(target);

    // check if the target tag is an icon, if so select the parent
    if (target.tagName === "I") {
        target = target.parentElement;
    }

    console.log(target);

    switch (target.getAttribute("data-action")) {
        case "new-folder":
            promptFolderCreation(document.getElementById("parentId").value);
            break;
        case "preview":
            showImagePreviewModal("/api/v1/files/" + getSelectedRows()[0].getAttribute("data-id"));
            break;
        case "share":
            showShareListModal(getSelectedRows()[0]);
            break;
        case "download":
            startFileDownload(getSelectedRows());
            break;
        case "edit":
            promptFileEdit(getSelectedRows()[0]);
            break;
        case "delete":
            const rows = getSelectedRows();
            if (rows.length >= 2 || (rows.length === 1 && rows[0].getAttribute("data-folder") === "true")) {
                showFileDeletionLoadingModal(rows.length);
            }
            $.ajax({
                url: "/api/v1/files",
                type: "DELETE",
                enctype: "multipart/form-data",
                data: convertRowsToXHRData(rows),
                processData: false,
                contentType: false,
                success: function (result) {
                    hideFileDeletionLoadingModal();
                    htmx.trigger("#refreshButton", "refreshTable");
                },
                error: function (error) {
                    // show fomantic toast
                    hideFileDeletionLoadingModal();
                    $('body')
                        .toast({
                            class: 'error',
                            message: 'Could not delete file: ' + error
                        })
                    ;
                }
            });
            break;
        default:
            return;
    }

    hideMenu();

    if (!isClickInside) {
        hideMenu();
    }
});

function convertRowsToXHRData(rows) {
    const ids = rows.map(function (row) {
        return row.dataset.id;
    }).join(",");
    const data = new FormData();
    data.append("files", ids);
    return data;
}

function startFileDownload(rows) {
    if (rows.length === 1 && !rows[0].getAttribute("data-folder")) {
        window.location.href = "/api/v1/files/" + rows[0].getAttribute("data-id") + "?download";
    } else {

        let fileIdList = convertRowsToXHRData(rows)
        showFileDownloadModal(rows.length, -1);
        // https://stackoverflow.com/a/29556434
        const xhr = new XMLHttpRequest();
        xhr.open("PUT", "/api/v1/files", true);
        xhr.setRequestHeader("x-client", "web/api");
        xhr.responseType = 'blob';
        // set multipart data
        xhr.onprogress = function (e) {
            // console.log(e.loaded + " / " + e.total);
            if (e.lengthComputable) {
                const percentComplete = (e.loaded / e.total) * 100;
                showFileDownloadModal(rows.length, Math.round(percentComplete));
            }
        };
        xhr.onload = function (e) {
            if (this.status === 200) {
                const filename = e.target.getResponseHeader("Content-Disposition").split(" ")[1].split("=")[1];
                const blob = new Blob([this.response], {type: 'application/zip'});
                const downloadUrl = URL.createObjectURL(blob);
                const a = document.createElement("a");
                a.href = downloadUrl;
                a.download = filename;
                document.body.appendChild(a);
                a.click();
            } else {
                const blob = new Blob([this.response], {type: 'application/zip'});
                const fileReader = new FileReader();
                fileReader.onload = (e) => {
                    $('body')
                        .toast({
                            class: 'red',
                            message: JSON.parse(e.target.result)[0],
                            position: 'bottom right',
                            displayTime: 5000,
                            showProgress: 'bottom',
                        })
                    ;
                };
                fileReader.readAsText(blob)

            }
            hideFileDownloadModal();
        };
        xhr.onabort = function (e) {
            $('body')
                .toast({
                    class: 'error',
                    message: 'Could not download files as zip'
                })
            ;
            hideFileDownloadModal();
        }
        xhr.send(fileIdList);
    }
}

function elementInsideTarget(element) {
    return element.closest("." + menuTargetClass) != null;
}

document.addEventListener("contextmenu", function (event) {
    var isClickInsideTable = elementInsideTarget(event.target);
    if (isClickInsideTable) {
        event.preventDefault();
        event.stopPropagation();
        showMenu(event.clientX, event.clientY, event.target.closest("tr"), document.getElementsByClassName("context-bounds")[0]);
        return false;
    } else if (!contextMenu.contains(event.target)) { // clicked inside the menu
        hideMenu();
    }
});

var timeoutId = null;

document.addEventListener("mousemove", function (event) {
    if (menuIsOpen()) {
        clientX = event.clientX;
        clientY = event.clientY;

        var mouseInsideMenu = contextMenu.contains(event.target);
        if (mouseInsideMenu) {
            stopTimeout();
        } else {
            // close menu after 1 second
            if (timeoutId == null) {
                timeoutId = setTimeout(function () {
                    hideMenu();
                }, 1000);
            }
        }
    }
});

function stopTimeout() {
    if (timeoutId != null) {
        clearTimeout(timeoutId);
        timeoutId = null;
    }
}

function menuIsOpen() {
    return contextMenu.classList.contains("visible");
}

var lastTarget = null;

function showMenu(x, y, target = null, related = null) {
    stopTimeout();

    // filter first before doing height calculations or anything else since they might be off
    filterMenuButtons(target.closest("table"), target);

    // bug: related height chagnes from first and second time
    let relativeClippingBounds = related.getBoundingClientRect();

    // check if it clips the bounds of the related element
    let clipX = x + contextMenu.offsetWidth > relativeClippingBounds.left + relativeClippingBounds.width;
    let clipY = y + contextMenu.offsetHeight > relativeClippingBounds.top + relativeClippingBounds.height;

    // dont clip if it would be smaller than the table
    if (relativeClippingBounds.height < contextMenu.offsetHeight) {
        clipY = false;
    }

    // calculate relative positions for the menu (since position: relative); its position is relative to its parent
    // this was the choice, so it scrolls with the rest
    let contextMenuRelativeBounds = contextMenu.parentElement.getBoundingClientRect();

    let relativeX = x - contextMenuRelativeBounds.left;
    let relativeY = y - contextMenuRelativeBounds.top;

    // make it fit inside the relative element
    let menuX = clipX ? relativeX - contextMenu.offsetWidth : relativeX;
    let menuY = clipY ? relativeY - contextMenu.offsetHeight : relativeY;

    // adjust for padding
    let computedStyle = getComputedStyle(contextMenu);

    // invert the padding if it clips
    menuX -= parseInt(computedStyle.paddingRight) * clipX ? -1 : 1;
    menuY -= parseInt(computedStyle.paddingBottom) * clipY ? -1 : 1;


    withoutTransition(contextMenu, parseInt(getComputedStyle(contextMenu).left) > 0, function () {
        contextMenu.style.left = menuX + "px";
        contextMenu.style.top = menuY + "px";
    });

    contextMenu.style.display = "block";
    contextMenu.classList.add("visible")

    if (related == null) {
        contextMenu.style.position = "fixed";
    } else {
        contextMenu.style.position = "absolute";
    }

    if (lastTarget != null && getSelectedRows().length === 1) {
        deselectRow(lastTarget);
    }

    if (target != null) {
        lastTarget = target;
        selectRow(target);
    }
}

function filterMenuButtons(table, target) {
    let tableId = table.getAttribute("id");
    // hide all buttons
    contextMenu.querySelectorAll(".item").forEach(function (item) {
        item.style.display = "none";
    });

    var itemType = "file";

    if (target.dataset.folder != null && target.dataset.folder == "true") {
        itemType = "folder";
    } else if (target.dataset.mime != null && target.dataset.mime.split("/")[0] === "image") {
        itemType = "image";
    }

    function showAction(action) {
        contextMenu.querySelector(".item[data-action='" + action + "']").style.display = "block";
    }

    // the order here doesn't matter but I try to keep it in the same order as the buttons are in the menu

    // showAction("new");
    showAction("new-folder");
    showAction("share");
    showAction("download");
    showAction("edit");
    showAction("move");
    showAction("delete");

    switch (itemType) {
        case "image":
            showAction("preview");
            break;
    }
}

function hideMenu() {
    stopTimeout();

    // contextMenu.style.display = "none";
    contextMenu.classList.remove("visible");

    if (lastTarget != null)
        lastTarget.classList.remove("active");

    // remove from viewport after the animation is done
    setTimeout(function () {
        withoutTransition(contextMenu, false, function () {
            contextMenu.style.left = -contextMenu.offsetWidth + "px";
            contextMenu.style.top = -contextMenu.offsetHeight + "px";
        });
    }, 100);

}

function withoutTransition(element, ignore = false, callback) {
    if (!ignore) {
        element.classList.add("notransition");
    }
    callback();

    if (!ignore) {
        element.offsetHeight;
        element.classList.remove("notransition");
    }
}

// DRAG & SELECT

let ds = null;

// custom drag drop handling

var customDragging = false;
var startElement = null;


document.addEventListener("dragstart", function (e) {
    if ($(e.target).hasClass("drag-startable")) {
        startElement = $(e.target).closest("tr").attr("data-id");
        customDragging = true;
        var elem = document.createElement("div");
        elem.id = "drag-ghost";
        elem.textNode = "Dragging";
        elem.classList.add("ui")
        elem.classList.add("inverted")
        elem.classList.add("segment")
        elem.style.position = "absolute";
        elem.style.top = "-1000px";

        elem.appendChild(document.createTextNode("Moving " + getSelectedRows().length + " item" + (getSelectedRows().length > 1 ? "s" : "")));

        document.body.appendChild(elem);
        e.dataTransfer.setDragImage(elem, -20, 0);
    }
}, false);

document.addEventListener("dragover", function (e) {
    if (customDragging) {
        const target = $(e.target)
        const closestTr = target.closest("tr")

        $("tr").removeClass("ds-drop-target");
        $(".section").removeClass("ds-drop-target");

        if (closestTr.length > 0 && closestTr.attr("data-folder") === "true" && closestTr.attr("data-id") !== startElement) {
            console.log(startElement)
            closestTr.addClass("ds-drop-target")
            e.preventDefault();
        } else if (target.hasClass("section")) {
            target.addClass("ds-drop-target")
            e.preventDefault();
        } else {

        }
    }
});

document.addEventListener("drop", function (e) {
    if (customDragging) {
        var targetId = "";
        const target = $(e.target)
        const closestTr = target.closest("tr")

        if (closestTr.length > 0 && closestTr.attr("data-folder") === "true") {
            targetId = closestTr.attr("data-id");
        } else {
            if (target.hasClass("section")) {
                targetId = target.attr("data-id");
            }
        }

        if (targetId !== "") {
            const filesToMove = getSelectedRows().map(x => $(x).attr("data-id"));

            if (filesToMove.indexOf(targetId) !== -1) {
                $('body')
                    .toast({
                        class: 'error',
                        message: "You can't move a folder into itself!"
                    })
                ;
                return;
            }
            const ids = filesToMove.join(",");
            const data = new FormData();
            data.append("files", ids);

            $.ajax({
                url: "/api/v1/files/" + targetId + "/setAsChildren",
                type: "POST",
                enctype: "multipart/form-data",
                data: data,
                processData: false,
                contentType: false,

                success: function (result) {
                    htmx.trigger("#refreshButton", "refreshTable");
                },
                error: function (error) {
                    $('body')
                        .toast({
                            class: 'error',
                            message: 'Could not delete file: ' + error
                        })
                    ;
                }
            });
            e.preventDefault();
        }
    }
});

// Let's remove the created ghost elem on dragend
document.addEventListener("dragend", function (e) {
    if (customDragging) {
        $("tr").removeClass("ds-drop-target");
        $(".section").removeClass("ds-drop-target");
        var ghost = document.getElementById("drag-ghost");
        if (ghost.parentNode) {
            ghost.parentNode.removeChild(ghost);
        }
    }
    customDragging = false;
}, false);


function initializeDragSelect() {
    ds = new DragSelect({
        selectables: document.getElementsByClassName('context-clickable'),
        area: document.getElementById('fileTableBody'),
        draggability: false, // implement this instead: https://interactjs.io/docs/
        keyboardDrag: false,
    });
    ds.subscribe('predragstart', ({event}) => {
        if (event.type != "mousedown") {
            ds.stop()
        }
        // only show custom mdoal when we are dragging a link; TODO: check if this is a custom element, or other things will cause this too.
        if (event.target instanceof HTMLAnchorElement) {
            const row = event.target.closest(".context-clickable");
            if (!isSelected(row)) {
                clearSelection()
                selectRow(row);
            }
            ds.break();
        }
    });
    ds.subscribe('dragstart', ({event}) => {
        if (event === "mousedown") {
            // console.log(event)
        }
        hideMenu();
    })
    ds.subscribe('elementselect', ({item, items}) => {
        // console.log(item)
        // $(item).find(".checkbox")[0].checked = true;

        selectRow(item);
        if (items.length == $(".context-clickable").length - 1) {
            $("#toggleAllSelection")[0].checked = true;
        }
    });
    ds.subscribe('elementunselect', ({item, items}) => {
        /// $(item).find(".checkbox")[0].checked = false;

        deselectRow(item);

        $("#toggleAllSelection")[0].checked = false;
        // console.log(items)
    });
}

initializeDragSelect();

function getSelectedRows() {
    return ds.getSelection()
}

function isSelected(row) {
    return ds.getSelection().indexOf(row) !== -1;
}

function toggleSelection() {
    if ($("#toggleAllSelection")[0].checked) {
        selectAll();
    } else {
        clearSelection();
    }
}

function selectAll() {
    $(".context-clickable").each(function (_, item) {
        selectRow(item)
    });
    //ds.addSelection($(".context-clickable"))
}

function clearSelection() {
    ds.getSelection().forEach(function (item) {
        deselectRow(item)
    });
}

function selectRow(row) {
    ds.addSelection(row)
    $(row).find(":checkbox")[0].checked = true;
}

function deselectRow(row) {
    ds.removeSelection(row)
    $(row).find(":checkbox")[0].checked = false;
}

function getAllRows() {
    return ds.getSelectables();
}