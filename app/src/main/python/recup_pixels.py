#!/usr/bin/env python3


import numpy as np
from PIL import Image, ImageOps, ImageEnhance
import os

def get_pixels_from_data(data):
    # `content` is already a bytes-like object, but if you need a standard bytes object:
    content = bytes(data)

    # If you need a file-like object:
    # import io
    # content_file = io.BytesIO(content)

    # If you need a filename (less efficient):
    import tempfile
    with tempfile.NamedTemporaryFile() as temp_file:
        temp_file.write(content)
        path = temp_file.name  # Valid only inside the `with` block.
        img = ImageOps.invert(Image.open(path).convert('L').resize((28, 28), Image.ANTIALIAS))

        # augmente le contraste
        facteur = 20
        img = ImageEnhance.Contrast(img).enhance(facteur)

        # rotation de la photo de 90 degré
        img = img.rotate(-90)

        img_pixels = np.array(img)

        x_test = [img_pixels]
        x_test = np.expand_dims(x_test, -1)

        return x_test


