import { Cart } from './cart.js';

class Products {
    static allProducts = [];
    static currentCategory = '';

    static async getAll() {
        try {
            const token = localStorage.getItem('authToken');
            console.log('Current auth token:', token);

            if (!token) {
                throw new Error('No authentication token found. Please log in again.');
            }

            const url = API_CONFIG.getFullUrl('PRODUCT', API_CONFIG.ENDPOINTS.PRODUCTS);
            console.log('Fetching products from:', url);

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                if (response.status === 401) {
                    Auth.logout();
                    throw new Error('Session expired. Please login again.');
                }
                const error = await response.json();
                throw new Error(error.message || 'Failed to fetch products');
            }

            const products = await response.json();
            this.allProducts = products; // Store products for filtering
            return products;
        } catch (error) {
            console.error('Error fetching products:', error);
            throw error;
        }
    }

    // Add this method to the Products class
    static async searchProducts(query) {
        try {
            const token = localStorage.getItem('authToken');
            if (!token) {
                throw new Error('No authentication token found');
            }

            const url = `${API_CONFIG.getFullUrl('PRODUCT', API_CONFIG.ENDPOINTS.SEARCH)}?query=${encodeURIComponent(query)}`;

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                if (response.status === 401) {
                    Auth.logout();
                    throw new Error('Session expired. Please login again.');
                }
                const error = await response.json();
                throw new Error(error.message || 'Failed to search products');
            }

            return await response.json();
        } catch (error) {
            console.error('Error searching products:', error);
            throw error;
        }
    }

    static async getCategories() {
        try {
            const token = localStorage.getItem('authToken');
            if (!token) {
                throw new Error('No authentication token found');
            }

            const url = API_CONFIG.getFullUrl('PRODUCT', '/api/categories');
            console.log('Fetching categories from:', url);

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to fetch categories');
            }

            return await response.json();
        } catch (error) {
            console.error('Error fetching categories:', error);
            return [];
        }
    }

    static async getProductsByCategory(categoryId = null) {
        try {
            const token = localStorage.getItem('authToken');
            if (!token) {
                throw new Error('No authentication token found. Please log in again.');
            }

            let url;
            if (categoryId) {
                url = API_CONFIG.getFullUrl('PRODUCT', `/api/products/category/${categoryId}`);
            } else {
                url = API_CONFIG.getFullUrl('PRODUCT', API_CONFIG.ENDPOINTS.PRODUCTS);
            }

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                if (response.status === 401) {
                    Auth.logout();
                    throw new Error('Session expired. Please login again.');
                }
                const error = await response.json();
                throw new Error(error.message || 'Failed to fetch products');
            }

            const products = await response.json();
            this.allProducts = products; // Store products for filtering
            return products;
        } catch (error) {
            console.error('Error fetching products:', error);
            throw error;
        }
    }

    static displayProducts(products) {
        const container = document.getElementById('products-container');
        if (!container) {
            console.error('Products container not found');
            return;
        }

        if (!Array.isArray(products) || products.length === 0) {
            container.innerHTML = '<div class="no-products">No products found</div>';
            return;
        }

        container.innerHTML = products.map(product => {
            const productData = product._source || product;
            const id = productData.id || productData.productId;
            const name = productData.name || 'Unnamed Product';
            const description = productData.description || 'No description available';
            const price = productData.price ? `$${Number(productData.price).toFixed(2)}` : '$0.00';
            const categoryName = productData.categoryName || productData.category?.name;
            const imageUrl = productData.imageUrls?.[0] || 'https://via.placeholder.com/300x200?text=No+Image';

            return `
                <div class="product-card" data-product='${JSON.stringify(productData).replace(/'/g, "\\'")}'>
                    <div class="product-card-inner">
                        <div class="product-image">
                            <img src="${imageUrl}" alt="${name}">
                        </div>
                        <div class="product-info">
                            <h3>${name}</h3>
                            <p class="price">${price}</p>
                            ${categoryName ? `<span class="category">${categoryName}</span>` : ''}
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        // Add click handlers to all product cards
        document.querySelectorAll('.product-card').forEach(card => {
            card.addEventListener('click', (e) => {
                // Don't trigger if clicking on a link or button inside the card
                if (e.target.tagName === 'A' || e.target.tagName === 'BUTTON' || e.target.closest('a, button')) {
                    return;
                }
                const productData = JSON.parse(card.dataset.product);
                this.showProductDetails(productData);
            });
        });
    }

        static showProductDetails(product) {
            // Create dialog container if it doesn't exist
            let dialog = document.getElementById('product-dialog');
            let overlay = document.getElementById('dialog-overlay');

            if (!dialog) {
                // Create overlay
                overlay = document.createElement('div');
                overlay.id = 'dialog-overlay';
                overlay.className = 'dialog-overlay';
                document.body.appendChild(overlay);

                // Create dialog
                dialog = document.createElement('div');
                dialog.id = 'product-dialog';
                dialog.className = 'product-dialog';
                document.body.appendChild(dialog);

                // Close dialog when clicking outside
                overlay.addEventListener('click', () => this.hideProductDialog());
            }

            // Show loading state
            dialog.innerHTML = `
                <div class="dialog-content">
                    <button class="dialog-close" aria-label="Close">&times;</button>
                    <div class="loading-container">
                        <div class="spinner"></div>
                        <p>Loading product details...</p>
                    </div>
                </div>
            `;

            // Add close button handler
            dialog.querySelector('.dialog-close').addEventListener('click', (e) => {
                e.stopPropagation();
                this.hideProductDialog();
            });

            // Show dialog and overlay
            dialog.style.display = 'block';
            overlay.style.display = 'block';
            document.body.style.overflow = 'hidden'; // Prevent scrolling

            // Load product details
            this.loadProductDetails(product.id)
                .then(productData => {
                    if (!productData) return;

                    const excludedFields = ['id', 'name', 'price', 'description', 'categoryName', 'imageUrls', 'createdAt', 'updatedAt', 'stockQuantity'];

                    const formatKey = (key) =>
                        key.replace(/([A-Z])/g, ' $1').replace(/^./, c => c.toUpperCase());

                    const formatValue = (value) => {
                        if (Array.isArray(value)) return value.join(', ');
                        if (value === null || value === undefined) return 'N/A';
                        if (typeof value === 'object') return JSON.stringify(value);
                        return value;
                    };

                    const specs = Object.entries(productData)
                        .filter(([key]) => !excludedFields.includes(key) && productData[key] !== null)
                        .map(([key, value]) => `
                            <tr>
                                <td class="spec-key">${formatKey(key)}</td>
                                <td class="spec-value">${formatValue(value)}</td>
                            </tr>
                        `).join('');

                    // In the dialog HTML template, update the quantity selector and add stock info
                    dialog.innerHTML = `
                        <div class="dialog-content">
                            <button class="dialog-close" aria-label="Close">&times;</button>
                            <div class="product-detail-container">
                                <div class="product-gallery">
                                    <div class="main-image">
                                        <img src="${productData.imageUrls?.[0] || 'https://via.placeholder.com/500'}"
                                             alt="${productData.name}"
                                             class="product-detail-image">
                                    </div>
                                </div>
                                <div class="product-info">
                                    <h2>${productData.name}</h2>
                                    <p class="price">$${Number(productData.price || 0).toFixed(2)}</p>
                                    <div class="product-description">
                                        <h3>Description</h3>
                                        <p>${productData.description || 'No description available.'}</p>
                                    </div>
                                    ${specs ? `
                                    <div class="product-specs">
                                        <h3>Specifications</h3>
                                        <table class="specs-table">
                                            <tbody>${specs}</tbody>
                                        </table>
                                    </div>
                                    ` : ''}
                                    <div class="stock-info">
                                        ${productData.stockQuantity > 5 ? 'In Stock' : `Only ${productData.stockQuantity} left in stock`}
                                    </div>
                                    <div class="product-actions">
                                        <div class="quantity-selector">
                                            <button type="button" class="quantity-btn minus" aria-label="Decrease quantity">-</button>
                                            <input type="number"
                                                   class="quantity-input"
                                                   value="1"
                                                   min="1"
                                                   max="${productData.stockQuantity || 99}"
                                                   aria-label="Quantity">
                                            <button type="button" class="quantity-btn plus" aria-label="Increase quantity">+</button>
                                        </div>
                                        <button class="btn btn-primary add-to-cart" data-product-id="${productData.id}">
                                            Add to Cart
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `;

                    // Add quantity selector functionality
                    const quantityInput = dialog.querySelector('.quantity-input');
                    const minusBtn = dialog.querySelector('.quantity-btn.minus');
                    const plusBtn = dialog.querySelector('.quantity-btn.plus');
                    const addToCartBtn = dialog.querySelector('.add-to-cart');
                    const maxQuantity = productData.stockQuantity || 99;

                    // Function to update quantity
                    function updateQuantity(change) {
                        let newValue = parseInt(quantityInput.value) + change;
                        newValue = Math.max(1, Math.min(maxQuantity, newValue));
                        quantityInput.value = newValue;
                        updateButtonStates();
                    }

                    // Update button states based on current quantity
                    function updateButtonStates() {
                        const currentValue = parseInt(quantityInput.value);
                        minusBtn.disabled = currentValue <= 1;
                        plusBtn.disabled = currentValue >= maxQuantity;

                        // Visual feedback when max is reached
                        if (currentValue >= maxQuantity) {
                            plusBtn.classList.add('disabled');
                        } else {
                            plusBtn.classList.remove('disabled');
                        }
                    }

                    // Event listeners
                    minusBtn.addEventListener('click', () => updateQuantity(-1));
                    plusBtn.addEventListener('click', () => updateQuantity(1));

                    // Handle manual input
                    quantityInput.addEventListener('input', (e) => {
                        let value = parseInt(e.target.value) || 1;
                        value = Math.max(1, Math.min(maxQuantity, value));
                        e.target.value = value;
                        updateButtonStates();
                    });

                    // Prevent non-numeric input
                    quantityInput.addEventListener('keydown', (e) => {
                        if (['e', 'E', '+', '-', '.'].includes(e.key)) {
                            e.preventDefault();
                        }
                    });

                    // Initial button state
                    updateButtonStates();

                    // Update add to cart functionality
                    addToCartBtn.addEventListener('click', async (e) => {
                        e.stopPropagation();

                        const quantity = parseInt(quantityInput.value, 10) || 1;
                        const productId = productData.id;

                        try {
                            await Cart.addToCart(productId, quantity);
                            this.hideProductDialog();
                        } catch (error) {
                            console.error('Error adding to cart:', error);
                            // addToCart already shows user-facing error
                        }
                    });

                    // Re-attach close button handler
                    dialog.querySelector('.dialog-close').addEventListener('click', (e) => {
                        e.stopPropagation();
                        this.hideProductDialog();
                    });
                })
                .catch(error => {
                    console.error('Error loading product details:', error);
                    dialog.innerHTML = `
                        <div class="dialog-content">
                            <button class="dialog-close" aria-label="Close">&times;</button>
                            <div class="error-container">
                                <p>Failed to load product details. Please try again.</p>
                                <button class="btn" onclick="Products.showProductDetails(${JSON.stringify(product)})">
                                    Retry
                                </button>
                            </div>
                        </div>
                    `;
                    dialog.querySelector('.dialog-close').addEventListener('click', (e) => {
                        e.stopPropagation();
                        this.hideProductDialog();
                    });
                });
        }

        static hideProductDialog() {
            const dialog = document.getElementById('product-dialog');
            const overlay = document.getElementById('dialog-overlay');

            if (dialog) dialog.style.display = 'none';
            if (overlay) overlay.style.display = 'none';
            document.body.style.overflow = ''; // Re-enable scrolling
        }

        static async loadProductDetails(productId) {
            try {
                const token = localStorage.getItem('authToken');
                if (!token) {
                    throw new Error('No authentication token found');
                }

                const productUrl = API_CONFIG.getFullUrl('PRODUCT', `/api/products/${productId}`);
                const response = await fetch(productUrl, {
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Accept': 'application/json'
                    }
                });

                if (!response.ok) {
                    throw new Error('Failed to fetch product details');
                }

                return await response.json();
            } catch (error) {
                console.error('Error loading product details:', error);
                throw error;
            }
        }

    static async initCategoryFilter() {
        try {
            const categories = await this.getCategories();
            const categorySelect = document.getElementById('category-filter');

            if (!categorySelect) return;

            // Clear existing options except the first one
            while (categorySelect.options.length > 1) {
                categorySelect.remove(1);
            }

            // Function to recursively add categories with indentation
            const addCategoriesToSelect = (categories, level = 0, parentId = null) => {
                // Filter categories by parent ID
                const children = categories.filter(cat =>
                    (parentId === null && !cat.parentId) ||
                    (cat.parentId && cat.parentId === parentId)
                );

                // Sort categories by name
                children.sort((a, b) => a.name.localeCompare(b.name));

                // Add each category and its children
                children.forEach(category => {
                    const option = document.createElement('option');
                    const indent = '&nbsp;'.repeat(level * 4);
                    const prefix = level > 0 ? '└── ' : '';
                    option.value = category.id;
                    option.innerHTML = `${indent}${prefix}${category.name}`;
                    option.style.paddingLeft = `${level * 10}px`;
                    categorySelect.appendChild(option);

                    // Add children recursively
                    addCategoriesToSelect(categories, level + 1, category.id);
                });
            };

            // Add categories to select with hierarchy
            addCategoriesToSelect(categories);

            // Add event listener for category change
            categorySelect.addEventListener('change', async (e) => {
                const categoryId = e.target.value;
                this.currentCategory = categoryId ? parseInt(categoryId) : null;

                try {
                    const products = await this.getProductsByCategory(this.currentCategory);
                    this.displayProducts(products);
                } catch (error) {
                    console.error('Error loading products:', error);
                    showMessage(error.message || 'Failed to load products', 'error');
                }
            });

            // Load initial products (all products)
            const initialProducts = await this.getProductsByCategory();
            this.displayProducts(initialProducts);

        } catch (error) {
            console.error('Error initializing category filter:', error);
            showMessage('Failed to load categories', 'error');
        }
    }

    static filterProductsByCategory() {
        if (!this.currentCategory) {
            // If no category selected, show all products
            this.displayProducts(this.allProducts);
            return;
        }

        // Filter products by selected category
        const filteredProducts = this.allProducts.filter(product => 
            (product.categoryId === this.currentCategory) || 
            (product.category && product.category.id === this.currentCategory) ||
            (product.category === this.currentCategory)
        );

        this.displayProducts(filteredProducts);
    }
}

export { Products };

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { Products };
}