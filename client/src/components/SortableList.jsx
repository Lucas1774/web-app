import PropTypes from "prop-types";
import { useState } from "react";
import { Button } from "react-bootstrap";
import { DndContext, closestCenter, PointerSensor, TouchSensor, useSensor, useSensors } from "@dnd-kit/core";
import { SortableContext, verticalListSortingStrategy, arrayMove, useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { NAME_KEY, CATEGORY_ID_KEY } from "../constants";

const DraggableItem = ({ item }) => {
    const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: item[CATEGORY_ID_KEY] });

    const style = {
        transform: CSS.Transform.toString(transform),
        transition,
        opacity: isDragging ? 0.5 : 1,
        cursor: "grab",
        touchAction: "none",
    };

    return (
        <div ref={setNodeRef} style={style} {...attributes} {...listeners} className="sortable-list">
            {item[NAME_KEY]}
        </div>
    );
};

DraggableItem.propTypes = {
    item: PropTypes.object.isRequired,
};

const SortableList = ({ items, onOrderSave, onItemMove }) => {
    const [localItems, setLocalItems] = useState(items);

    const pointerSensor = useSensor(PointerSensor, {
        activationConstraint: {
            distance: 8,
        },
    });
    const touchSensor = useSensor(TouchSensor, {
        activationConstraint: {
            delay: 150,
            tolerance: 5,
        },
    });

    const sensors = useSensors(/Android/i.test(navigator.userAgent) ? touchSensor : pointerSensor);

    const handleDragEnd = (event) => {
        const { active, over } = event;
        if (!over || active.id === over.id) {
            return;
        }
        const oldIndex = localItems.findIndex(
            (item) => item[CATEGORY_ID_KEY] === active.id
        );
        const newIndex = localItems.findIndex(
            (item) => item[CATEGORY_ID_KEY] === over.id
        );

        const newItems = arrayMove(localItems, oldIndex, newIndex);
        setLocalItems(newItems);
        onItemMove(oldIndex, newIndex);
    };

    return localItems.length === 0 ? (
        <div>No categories</div>
    ) : (
        <>
            <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
                <SortableContext
                    items={localItems.map((item) => item[CATEGORY_ID_KEY])}
                    strategy={verticalListSortingStrategy}
                >
                    {localItems.map((item) => (
                        <DraggableItem key={item[CATEGORY_ID_KEY]} item={item} />
                    ))}
                </SortableContext>
            </DndContext>
            <Button variant="success" onClick={onOrderSave}>
                Save
            </Button>
        </>
    );
};

SortableList.propTypes = {
    items: PropTypes.array.isRequired,
    onOrderSave: PropTypes.func.isRequired,
    onItemMove: PropTypes.func,
};

export default SortableList;
